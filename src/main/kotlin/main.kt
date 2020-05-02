import org.khronos.webgl.ArrayBuffer
import org.w3c.dom.Audio
import org.w3c.dom.Document
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLInputElement
import org.w3c.xhr.ARRAYBUFFER
import org.w3c.xhr.XMLHttpRequest
import org.w3c.xhr.XMLHttpRequestResponseType
import kotlin.browser.document
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

external class GainNode: AudioNode {
    val gain: AudioParam
}

class Ui(doc: Document) {

    val startBtn = doc.getElementById("startButton") as HTMLButtonElement
    val stopBtn =  doc.getElementById("stopButton") as HTMLButtonElement

    val volDrum = doc.getElementById("drumsVol") as HTMLInputElement
    val volBass = doc.getElementById("bassVol") as HTMLInputElement
    val volGuitar = doc.getElementById("guitarVol") as HTMLInputElement
    val volKeys = doc.getElementById("keysVol") as HTMLInputElement
}

class AudioGraph(val ui: Ui) {

    init {
        ui.startBtn.addEventListener("click", { setupAndPlay() })
        ui.stopBtn.addEventListener("click", { stop() })
    }

    var sources: Array<out AudioBufferSourceNode>? = null
    var context: AudioContext? = null

    private fun setupAndPlay() {

        if (context != null) return
        val ctx = AudioContext()

        // Prepare the tracks:

        Promise.all(arrayOf(
                createTrack("drums.m4a", ctx, ui.volDrum),
                createTrack("bass.m4a", ctx, ui.volBass),
                createTrack("guitar.m4a", ctx, ui.volGuitar),
                createTrack("keys.m4a", ctx, ui.volKeys)
        )).then { sources ->

            // When everything is ready, start playback:

            sources.forEach { it.start(0.0) }
            this.sources = sources
            this.context = ctx
        }
    }

    fun createTrack(
            clip: String, ctx: AudioContext, volSlider: HTMLInputElement
    ) = Promise<AudioBufferSourceNode> { onResolved, onRejected ->

        // Setup the buffer-source -> gain -> audio out graph:

        val source = ctx.createBufferSource()
        val gain = ctx.createGain()
        source.connect(gain).connect(ctx.destination)

        // Hook the gain value to the provided slider:

        gain.gain.value = 0.6f //volSlider.valueAsNumber.toFloat()

        volSlider.onchange = {
            gain.gain.value = volSlider.valueAsNumber.toFloat()
            Unit
        }

        // Load the desired audio clip (GET it and decode the raw data buffer):
        // TODO: audio buffers are reusable, load+decode them only once

        XMLHttpRequest().apply {

            open("GET", clip, true)
            responseType = XMLHttpRequestResponseType.ARRAYBUFFER

            onerror = {
                onRejected(Exception("$clip request error: $it"))
            }

            onload = {

                val encodedData = response as? ArrayBuffer

                if (encodedData == null) {
                    println("Null audio data from response!")
                } else {

                    // once we got the array buffer with the clip data
                    // we need to decode that, and set the result into the
                    // BufferSourceNode:

                    ctx.decodeAudioData(encodedData).then { decoded ->
                        source.buffer = decoded
                        source.loop = true
                        onResolved(source)
                    }
                }
            }

            // Fire the request:
            send()
        }
    }

    private fun stop() {
        context?.let {
            sources?.forEach { it.stop() }
            sources = null
            it.close()
            context = null
        }
    }
}

fun main() {
    val ui = Ui(document)
    AudioGraph(ui)
}