import kotlinx.coroutines.*
import org.w3c.dom.Document
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLInputElement
import kotlin.browser.document
import kotlin.browser.window

class Ui(doc: Document) {

    val startBtn = doc.getElementById("startButton") as HTMLButtonElement
    val stopBtn = doc.getElementById("stopButton") as HTMLButtonElement

    val volDrum = doc.getElementById("drumsVol") as HTMLInputElement
    val volBass = doc.getElementById("bassVol") as HTMLInputElement
    val volGuitar = doc.getElementById("guitarVol") as HTMLInputElement
    val volKeys = doc.getElementById("keysVol") as HTMLInputElement
}

class AudioGraph(private val ui: Ui) {

    private var startJob: Job? = null

    init {
        ui.startBtn.onclick = start@{
            if (startJob?.isActive == true) return@start false
            // It allows to make start cancellable
            startJob = GlobalScope.launch { setupAndPlay() }
            true
        }
        ui.stopBtn.onclick = {
            startJob?.cancel()
            stop()
        }
    }

    private var sources: List<AudioBufferSourceNode>? = null
    private var context: AudioContext? = null

    private suspend fun setupAndPlay() {

        if (context != null) return
        val ctx = AudioContext()

        // Prepare the tracks:

        val tracks = coroutineScope {
            awaitAll(
                    async { createTrack("drums.m4a", ctx, ui.volDrum) },
                    async { createTrack("bass.m4a", ctx, ui.volBass) },
                    async { createTrack("guitar.m4a", ctx, ui.volGuitar) },
                    async { createTrack("keys.m4a", ctx, ui.volKeys) }
            )
        }
        tracks.forEach { it.start(0.0) }
        sources = tracks
        context = ctx
    }

    private suspend fun createTrack(
        clip: String, ctx: AudioContext, volSlider: HTMLInputElement
    ): AudioBufferSourceNode {

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

        val encodedData = window.fetch(clip).await().arrayBuffer().await()
        // once we got the array buffer with the clip data
        // we need to decode that, and set the result into the
        // BufferSourceNode:
        source.buffer = ctx.decodeAudioData(encodedData).await()
        source.loop = true

        return source
    }

    private fun stop() {
        sources?.forEach { it.stop() }
        sources = null

        context?.close()
        context = null
    }
}

fun main() {
    window.onload = {
        val ui = Ui(document)
        AudioGraph(ui)
    }
}