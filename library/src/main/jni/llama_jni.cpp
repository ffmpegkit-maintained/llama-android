// JNI bridge between the Kotlin API (dev.ffmpegkit.llama.LlamaJNI) and llama.cpp.
// Uses the core llama.h API only (no examples "common" lib). Produces
// libllama_jni.so; Kotlin loads ggml, llama, then llama_jni.
//
// Native entry points (must match LlamaJNI.kt):
//   nativeLoadModel(path, nCtx, nThreads, nGpuLayers)                 -> jlong handle
//   nativeComplete(handle, prompt, system, maxTokens, temp, topP, topK, seed) -> jstring (JSON)
//   nativeEmbed(handle, text)                                         -> jfloatArray
//   nativeReleaseModel(handle)                                        -> void
//   nativeGetSystemInfo()                                             -> jstring

#include <jni.h>
#include <android/log.h>
#include <string>
#include <vector>

#include "llama.h"

#define LOG_TAG "llama-jni"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace {

struct LlamaCtx {
    llama_model *model = nullptr;
    llama_context *ctx = nullptr;
    int n_threads = 4;
};

bool g_backend_ready = false;

std::string jstr(JNIEnv *env, jstring s) {
    if (!s) return {};
    const char *c = env->GetStringUTFChars(s, nullptr);
    std::string out = c ? c : "";
    if (c) env->ReleaseStringUTFChars(s, c);
    return out;
}

std::string json_escape(const std::string &in) {
    std::string o;
    o.reserve(in.size() + 8);
    for (char c : in) {
        switch (c) {
            case '"':  o += "\\\""; break;
            case '\\': o += "\\\\"; break;
            case '\n': o += "\\n";  break;
            case '\r': o += "\\r";  break;
            case '\t': o += "\\t";  break;
            default:   o += c;      break;
        }
    }
    return o;
}

std::vector<llama_token> tokenize(const llama_vocab *vocab, const std::string &text,
                                  bool add_special) {
    int n = -llama_tokenize(vocab, text.c_str(), (int32_t) text.size(),
                            nullptr, 0, add_special, true);
    std::vector<llama_token> tokens(n);
    int check = llama_tokenize(vocab, text.c_str(), (int32_t) text.size(),
                               tokens.data(), (int32_t) tokens.size(), add_special, true);
    if (check < 0) tokens.clear();
    return tokens;
}

std::string token_to_piece(const llama_vocab *vocab, llama_token token) {
    char buf[256];
    int n = llama_token_to_piece(vocab, token, buf, sizeof(buf), 0, true);
    if (n < 0) return {};
    return std::string(buf, n);
}

// Format the (system,user) turn with the model's built-in chat template, falling
// back to a plain concatenation if the model has none.
std::string build_prompt(const llama_model *model, const std::string &system,
                         const std::string &user) {
    const char *tmpl = llama_model_chat_template(model, nullptr);
    if (!tmpl) {
        return system.empty() ? user : (system + "\n\n" + user);
    }
    std::vector<llama_chat_message> msgs;
    if (!system.empty()) msgs.push_back({"system", system.c_str()});
    msgs.push_back({"user", user.c_str()});
    std::vector<char> buf(user.size() + system.size() + 2048);
    int32_t n = llama_chat_apply_template(tmpl, msgs.data(), msgs.size(), true,
                                          buf.data(), (int32_t) buf.size());
    if (n > (int32_t) buf.size()) {
        buf.resize(n);
        n = llama_chat_apply_template(tmpl, msgs.data(), msgs.size(), true,
                                      buf.data(), (int32_t) buf.size());
    }
    if (n < 0) return system.empty() ? user : (system + "\n\n" + user);
    return std::string(buf.data(), n);
}

llama_sampler *make_sampler(float temp, float topP, int topK, int seed) {
    llama_sampler *smpl = llama_sampler_chain_init(llama_sampler_chain_default_params());
    if (temp <= 0.0f) {
        llama_sampler_chain_add(smpl, llama_sampler_init_greedy());
    } else {
        llama_sampler_chain_add(smpl, llama_sampler_init_top_k(topK));
        llama_sampler_chain_add(smpl, llama_sampler_init_top_p(topP, 1));
        llama_sampler_chain_add(smpl, llama_sampler_init_temp(temp));
        llama_sampler_chain_add(smpl, llama_sampler_init_dist(
                seed < 0 ? LLAMA_DEFAULT_SEED : (uint32_t) seed));
    }
    return smpl;
}

} // namespace

extern "C" {

JNIEXPORT jlong JNICALL
Java_dev_ffmpegkit_llama_LlamaJNI_nativeLoadModel(
        JNIEnv *env, jobject, jstring path, jint nCtx, jint nThreads, jint nGpuLayers) {
    if (!g_backend_ready) {
        llama_backend_init();
        g_backend_ready = true;
    }
    auto *h = new LlamaCtx();
    h->n_threads = nThreads;

    llama_model_params mp = llama_model_default_params();
    mp.n_gpu_layers = nGpuLayers;
    h->model = llama_model_load_from_file(jstr(env, path).c_str(), mp);
    if (!h->model) {
        LOGE("failed to load model");
        delete h;
        return 0;
    }

    llama_context_params cp = llama_context_default_params();
    cp.n_ctx = (uint32_t) nCtx;
    cp.n_threads = nThreads;
    cp.n_threads_batch = nThreads;
    h->ctx = llama_init_from_model(h->model, cp);
    if (!h->ctx) {
        LOGE("failed to create context");
        llama_model_free(h->model);
        delete h;
        return 0;
    }
    return reinterpret_cast<jlong>(h);
}

JNIEXPORT jstring JNICALL
Java_dev_ffmpegkit_llama_LlamaJNI_nativeComplete(
        JNIEnv *env, jobject, jlong handle, jstring prompt, jstring system,
        jint maxTokens, jfloat temp, jfloat topP, jint topK, jint seed) {
    auto *h = reinterpret_cast<LlamaCtx *>(handle);
    if (!h || !h->ctx) return env->NewStringUTF("{}");

    const llama_vocab *vocab = llama_model_get_vocab(h->model);
    const std::string text = build_prompt(h->model, jstr(env, system), jstr(env, prompt));

    // Each complete() is a fresh single turn: reset the KV cache so successive calls
    // on the same model don't bleed context into one another.
    llama_memory_clear(llama_get_memory(h->ctx), true);

    std::vector<llama_token> tokens = tokenize(vocab, text, true);
    const int n_prompt = (int) tokens.size();

    llama_sampler *smpl = make_sampler(temp, topP, topK, seed);

    const int64_t t0 = llama_time_us();
    llama_batch batch = llama_batch_get_one(tokens.data(), (int32_t) tokens.size());
    if (llama_decode(h->ctx, batch) != 0) {
        llama_sampler_free(smpl);
        return env->NewStringUTF("{\"text\":\"\",\"error\":\"decode failed\"}");
    }
    const int64_t t1 = llama_time_us();

    std::string out;
    int n_gen = 0;
    for (; n_gen < maxTokens; n_gen++) {
        llama_token id = llama_sampler_sample(smpl, h->ctx, -1);
        if (llama_vocab_is_eog(vocab, id)) break;
        llama_sampler_accept(smpl, id);
        out += token_to_piece(vocab, id);
        llama_batch nb = llama_batch_get_one(&id, 1);
        if (llama_decode(h->ctx, nb) != 0) break;
    }
    const int64_t t2 = llama_time_us();

    llama_sampler_free(smpl);

    const long prompt_ms = (long) ((t1 - t0) / 1000);
    const long gen_ms = (long) ((t2 - t1) / 1000);
    const float tps = gen_ms > 0 ? (n_gen * 1000.0f / gen_ms) : 0.0f;

    std::string json = "{\"text\":\"" + json_escape(out) +
        "\",\"tokensGenerated\":" + std::to_string(n_gen) +
        ",\"promptTokens\":" + std::to_string(n_prompt) +
        ",\"tokensPerSecond\":" + std::to_string(tps) +
        ",\"promptEvalTimeMs\":" + std::to_string(prompt_ms) +
        ",\"generateTimeMs\":" + std::to_string(gen_ms) + "}";
    return env->NewStringUTF(json.c_str());
}

JNIEXPORT jfloatArray JNICALL
Java_dev_ffmpegkit_llama_LlamaJNI_nativeEmbed(
        JNIEnv *env, jobject, jlong handle, jstring text) {
    auto *h = reinterpret_cast<LlamaCtx *>(handle);
    if (!h || !h->ctx) return env->NewFloatArray(0);

    const llama_vocab *vocab = llama_model_get_vocab(h->model);
    std::vector<llama_token> tokens = tokenize(vocab, jstr(env, text), true);
    const int n_tok = (int) tokens.size();

    // Embeddings must be explicitly enabled or llama_get_embeddings_* returns null.
    // Toggle it on for this call and restore generation mode afterwards.
    llama_set_embeddings(h->ctx, true);
    llama_memory_clear(llama_get_memory(h->ctx), true);
    llama_batch batch = llama_batch_get_one(tokens.data(), (int32_t) n_tok);
    if (llama_decode(h->ctx, batch) != 0) {
        llama_set_embeddings(h->ctx, false);
        return env->NewFloatArray(0);
    }

    const int n_embd = llama_model_n_embd(h->model);
    // Pooled sequence embedding (embedding models); fall back to the last token's
    // hidden state (plain LLMs with no pooling configured).
    const float *emb = llama_get_embeddings_seq(h->ctx, 0);
    if (!emb) emb = llama_get_embeddings_ith(h->ctx, n_tok - 1);
    if (!emb) emb = llama_get_embeddings(h->ctx);
    llama_set_embeddings(h->ctx, false);
    if (!emb) return env->NewFloatArray(0);

    jfloatArray arr = env->NewFloatArray(n_embd);
    env->SetFloatArrayRegion(arr, 0, n_embd, emb);
    return arr;
}

JNIEXPORT void JNICALL
Java_dev_ffmpegkit_llama_LlamaJNI_nativeReleaseModel(JNIEnv *, jobject, jlong handle) {
    auto *h = reinterpret_cast<LlamaCtx *>(handle);
    if (!h) return;
    if (h->ctx) llama_free(h->ctx);
    if (h->model) llama_model_free(h->model);
    delete h;
}

JNIEXPORT jstring JNICALL
Java_dev_ffmpegkit_llama_LlamaJNI_nativeGetSystemInfo(JNIEnv *env, jobject) {
    return env->NewStringUTF(llama_print_system_info());
}

} // extern "C"
