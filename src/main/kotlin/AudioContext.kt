import org.khronos.webgl.ArrayBuffer
import kotlin.js.Promise

external class AudioContext {

    val sampleRate: Float
    val destination: AudioNode
    val baseLatency: Double // seconds, experimental
    val outputLatency: Double // seconds, experimental

    //val listener
    //val state

    //fun suspend(): Promise
    //fun resume(): Promise
    fun close()

    fun createOscillator(): OscillatorNode

    //    fun createBuffer(): AudioBuffer
    fun createBufferSource(): AudioBufferSourceNode
    fun decodeAudioData(data: ArrayBuffer): Promise<AudioBuffer>

    fun createGain(): GainNode


//    fun createAnalyser(): AnalyserNode
//    fun createBiquadFilter(): BiquadFilterNode
//    fun createConstantSource(): ConstantSourceNode
//    fun createChannelMerger(): ChannelMergerNode
//    fun createChannelSplitter(): ChannelSplitterNode
//    fun createConvolver(): ConvolverNode
//    fun createDelay(): DelayNode
//    fun createDynamicsCompressor(): DynamicsCompressorNode
//    fun createIIRFilter(): IIRFilterNode
//    fun createPanner(): PannerNode
//    fun createPeriodicWave(): PeriodicWave
//    fun createStereoPanner(): StereoPannerNode
//    fun createWaveShaper(): WaveShaperNode

}

external open class AudioNode {
    fun connect(destination: AudioNode, output: Int = definedExternally, input: Int = definedExternally): AudioNode
}

external class OscillatorNode : AudioNode {
    fun start(time: Double = definedExternally)
}

external class AudioBuffer

external class AudioBufferSourceNode : AudioNode {
    fun start(time: Double = definedExternally)
    fun stop(time: Double = definedExternally)
    var buffer: AudioBuffer
    var loop: Boolean
}

external class AudioParam {
    val defaultVaue: Float
    val maxValue: Float
    val minValue: Float
    var value: Float
}

external class GainNode : AudioNode {
    val gain: AudioParam
}
