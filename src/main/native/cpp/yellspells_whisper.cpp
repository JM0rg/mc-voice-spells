#include <jni.h>
#include <string>
#include <memory>
#include <vector>
#include <cstring>
#include <whisper.h>

// JNI function implementations for YellSpells Whisper integration

struct WhisperContext {
    whisper_context* ctx;
    std::vector<float> audio_buffer;
    std::string last_result;
    
    WhisperContext(whisper_context* c) : ctx(c) {
        audio_buffer.reserve(32000); // Reserve space for ~2 seconds at 16kHz
    }
    
    ~WhisperContext() {
        if (ctx) {
            whisper_free(ctx);
        }
    }
};

extern "C" {

JNIEXPORT jlong JNICALL
Java_com_yellspells_client_stt_WhisperJNI_nativeInit(JNIEnv* env, jclass clazz, jstring modelPath, jint sampleRate, jint threads) {
    const char* path = env->GetStringUTFChars(modelPath, nullptr);
    
    // Initialize whisper context with new API
    whisper_context_params ctx_params = whisper_context_default_params();
    whisper_context* ctx = whisper_init_from_file_with_params(path, ctx_params);
    env->ReleaseStringUTFChars(modelPath, path);
    
    if (!ctx) {
        return 0; // Failed to initialize
    }
    
    // Create our wrapper context
    WhisperContext* wrapper = new WhisperContext(ctx);
    return reinterpret_cast<jlong>(wrapper);
}

JNIEXPORT jint JNICALL
Java_com_yellspells_client_stt_WhisperJNI_nativePush(JNIEnv* env, jclass clazz, jlong contextPtr, jobject audioBuffer, jint samples) {
    if (contextPtr == 0) return -1;
    
    WhisperContext* wrapper = reinterpret_cast<WhisperContext*>(contextPtr);
    
    // Get direct buffer
    float* audio_data = static_cast<float*>(env->GetDirectBufferAddress(audioBuffer));
    if (!audio_data) return -1;
    
    // Append to our audio buffer
    wrapper->audio_buffer.insert(wrapper->audio_buffer.end(), audio_data, audio_data + samples);
    
    return 0; // Success
}

JNIEXPORT jint JNICALL
Java_com_yellspells_client_stt_WhisperJNI_nativePoll(JNIEnv* env, jclass clazz, jlong contextPtr, jobject outBuffer, jfloatArray confStable) {
    if (contextPtr == 0) return 0;
    
    WhisperContext* wrapper = reinterpret_cast<WhisperContext*>(contextPtr);
    
    // Check if we have enough audio (at least 1 second at 16kHz)
    if (wrapper->audio_buffer.size() < 16000) {
        return 0; // Not enough audio yet
    }
    
    // Set up whisper parameters
    whisper_full_params params = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
    params.language = "en";
    params.n_threads = 4;
    params.translate = false;
    params.print_realtime = false;
    params.print_progress = false;
    params.print_timestamps = false;
    params.print_special = false;
    params.single_segment = true;
    
    // Process audio
    int result = whisper_full(wrapper->ctx, params, wrapper->audio_buffer.data(), wrapper->audio_buffer.size());
    
    if (result != 0) {
        return 0; // Processing failed
    }
    
    // Get the transcription
    int n_segments = whisper_full_n_segments(wrapper->ctx);
    if (n_segments == 0) {
        return 0; // No segments
    }
    
    // Get the last segment
    const char* text = whisper_full_get_segment_text(wrapper->ctx, n_segments - 1);
    if (!text || strlen(text) == 0) {
        return 0; // No text
    }
    
    // Copy result to Java buffer
    int text_len = strlen(text);
    jbyte* buffer = static_cast<jbyte*>(env->GetDirectBufferAddress(outBuffer));
    if (!buffer) return 0;
    
    jlong buffer_capacity = env->GetDirectBufferCapacity(outBuffer);
    if (text_len > buffer_capacity) {
        text_len = buffer_capacity; // Truncate if too long
    }
    
    memcpy(buffer, text, text_len);
    
    // Set confidence (Whisper doesn't provide confidence directly, so we estimate based on segment probability)
    jfloat* conf_array = env->GetFloatArrayElements(confStable, nullptr);
    if (conf_array) {
        // Simple confidence estimation - in real implementation you'd calculate this properly
        conf_array[0] = 0.8f; // Confidence
        conf_array[1] = 1.0f; // Stable (assume stable for single segment)
        env->ReleaseFloatArrayElements(confStable, conf_array, 0);
    }
    
    // Clear processed audio (keep some overlap for continuity)
    if (wrapper->audio_buffer.size() > 8000) { // Keep last 0.5 seconds
        wrapper->audio_buffer.erase(wrapper->audio_buffer.begin(), wrapper->audio_buffer.end() - 8000);
    }
    
    return text_len;
}

JNIEXPORT void JNICALL
Java_com_yellspells_client_stt_WhisperJNI_nativeClose(JNIEnv* env, jclass clazz, jlong contextPtr) {
    if (contextPtr != 0) {
        WhisperContext* wrapper = reinterpret_cast<WhisperContext*>(contextPtr);
        delete wrapper;
    }
}

} // extern "C"
