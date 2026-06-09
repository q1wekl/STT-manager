package app.service;

import app.model.SilenceRegion;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SilenceDetectorService {
    private final String ffmpegPath = "bin/ffmpeg.exe";
    public List<SilenceRegion> detect(File wavFile){
        try{
            ProcessBuilder pb = new ProcessBuilder(
                    ffmpegPath,
                    "-i", wavFile.getAbsolutePath(),
                    "-af", "silencedetect=noise=-35dB:d=0.5",
                    "-f", "null",
                    "-"
            );
            Process process = pb.start();
            String log = new String(process.getErrorStream().readAllBytes());
            process.waitFor();
            return parseSilence(log);


        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }
    private List<SilenceRegion> parseSilence(String log){
        List<SilenceRegion> result = new ArrayList<>();
        Pattern startPattern  = Pattern.compile("silence_start: ([0-9.]+)");
        Pattern endPattern  = Pattern.compile("silence_end: ([0-9.]+)");
        Double currentStart = null;
        for(String line: log.split("\n")){
            Matcher startMatcher = startPattern.matcher(line);
            Matcher endMatcher = endPattern.matcher(line);
            if(startMatcher.find()){
                currentStart = Double.parseDouble(startMatcher.group(1));
            }
            if(endMatcher.find() && currentStart != null){
                double end = Double.parseDouble(endMatcher.group(1));
                result.add(new SilenceRegion(currentStart, end));
                currentStart = null;
            }
        }
        return result;
    }
}
