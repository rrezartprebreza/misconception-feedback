package dev.rrezart.misconception.feedback;

import dev.rrezart.misconception.model.Submission;

import java.util.regex.Pattern;

/**
 * Removes obvious personal data before anything is sent to a model: email addresses and
 * "@author" lines. It is not a guarantee, so review a sample of submissions by hand.
 */
public class Anonymiser {

    private static final Pattern EMAIL = Pattern.compile("[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+");
    private static final Pattern AUTHOR = Pattern.compile("@author[^\\r\\n]*");

    public Submission anonymise(Submission submission) {
        return new Submission(
                submission.id(),
                submission.task(),
                clean(submission.code()),
                clean(submission.explanation()));
    }

    String clean(String text) {
        if (text == null) {
            return null;
        }
        String withoutAuthor = AUTHOR.matcher(text).replaceAll("@author [removed]");
        return EMAIL.matcher(withoutAuthor).replaceAll("[email removed]");
    }
}
