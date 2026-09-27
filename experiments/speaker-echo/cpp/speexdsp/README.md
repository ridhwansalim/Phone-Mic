# Vendored SpeexDSP 1.2.1

Upstream: https://downloads.xiph.org/releases/speex/speexdsp-1.2.1.tar.gz

Archive SHA-256: `8c777343e4a6399569c72abc38a95b24db56882c83dbdb6c6424a5f4aeb54d3d`

Only the echo canceller, preprocessor, filter bank, KISS FFT source files and supporting headers are compiled. Upstream source files are unmodified. `include/speex/speexdsp_config_types.h` supplies fixed-width Android types. The parent `config.h`, `CMakeLists.txt` and `echo_jni.c` are Phone Mic integration code. Floating-point KISS FFT, no automatic gain control, denoising disabled, residual echo suppression -24 dB / -10 dB during near speech. Ten-millisecond frames at 48 kHz and a 200 ms acoustic filter tail.

See COPYING and individual source headers for licenses. The earlier experimental APKs included both SpeexDSP and KISS FFT notices. Current v1.4 build 9 excludes this engine from the APK; the combined notices are retained at experiments/speaker-echo/SpeexDSP-LICENSE.txt.
