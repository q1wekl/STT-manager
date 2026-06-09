package app.concurent;

import app.model.AudioChunk;
import app.model.SilenceRegion;
import app.service.AudioChunkerService;
import app.service.AudioDecoder;
import app.service.SilenceDetectorService;

import java.io.File;
import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;

public class AudioChunkProducer implements Callable<Integer> {

    private final File wavFile;
    private final BlockingQueue<AudioChunk> chunkQueue;
    private final AudioDecoder decoder;
    private final AudioChunkerService chunker;
    private final SilenceDetectorService detector;

    public AudioChunkProducer(File wavFile, BlockingQueue<AudioChunk> chunkQueue, AudioDecoder decoder, AudioChunkerService chunker, SilenceDetectorService detector) {
        this.wavFile = wavFile;
        this.chunkQueue = chunkQueue;
        this.decoder = decoder;
        this.chunker = chunker;
        this.detector = detector;
    }

    @Override
    public Integer call() throws Exception {

        List<SilenceRegion> silences = detector.detect(wavFile);
        float[] audio = decoder.decode(wavFile);
        List<AudioChunk> chunks = chunker.createChunks(audio, silences);

        for (AudioChunk chunk : chunks) {
            chunkQueue.put(chunk);
        }

        return chunks.size();
    }
}
