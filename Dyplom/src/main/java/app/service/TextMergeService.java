package app.service;

public class TextMergeService {
    public String merge(String previous, String current) {

        if (previous.isBlank()) {
            return current;
        }

        String prev = previous.trim();
        String curr = current.trim();

        int maxOverlap = Math.min(prev.length(), curr.length());

        for (int len = maxOverlap; len > 10; len--) {

            String prevEnd = prev.substring(prev.length() - len);
            String currStart = curr.substring(0, len);

            if (prevEnd.equalsIgnoreCase(currStart)) {
                return prev + curr.substring(len);
            }
        }

        return prev + "\n" + curr;
    }
}
