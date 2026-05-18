package app.service;

import java.io.File;

public class FFmpegConverter {
    private final String ffmpegPath = "bin/ffmpeg.exe";
    public File convertToWav(File input){
        File output = new File("temp/audio_" + System.currentTimeMillis() + ".wav");
        output.getParentFile().mkdirs();

        try{
            ProcessBuilder pb = new ProcessBuilder(
                    ffmpegPath,
                    "-y",
                    "-i", input.getAbsolutePath(),
                    "-ar", "16000",
                    "-ac", "1",
                    "-f", "wav",
                    output.getAbsolutePath()
            );

            Process process= pb.start();
            int code = process.waitFor();
            if(code!=0){
                throw new RuntimeException("FFmpeg failed: "+ code);
            }
            return output;

        }catch(Exception e){
            throw new RuntimeException("FFmpeg error: ",e);
        }
    }
}
