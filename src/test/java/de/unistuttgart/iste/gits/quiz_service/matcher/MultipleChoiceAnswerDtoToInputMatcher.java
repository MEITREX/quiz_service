package de.unistuttgart.iste.gits.quiz_service.matcher;

import de.unistuttgart.iste.meitrex.generated.dto.MultipleChoiceAnswer;
import de.unistuttgart.iste.meitrex.generated.dto.MultipleChoiceAnswerInput;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;

import java.util.Objects;

/**
 * Matcher for comparing a {@link MultipleChoiceAnswer} to a {@link MultipleChoiceAnswerInput}.
 */
public class MultipleChoiceAnswerDtoToInputMatcher extends TypeSafeDiagnosingMatcher<MultipleChoiceAnswer> {

    private final MultipleChoiceAnswerInput expected;

    public MultipleChoiceAnswerDtoToInputMatcher(final MultipleChoiceAnswerInput expected) {
        this.expected = expected;
    }

    public static MultipleChoiceAnswerDtoToInputMatcher matchesInput(final MultipleChoiceAnswerInput expected) {
        return new MultipleChoiceAnswerDtoToInputMatcher(expected);
    }

    @Override
    protected boolean matchesSafely(final MultipleChoiceAnswer item, final Description mismatchDescription) {
        if (!item.getAnswerText().equals(expected.getAnswerText())) {
            mismatchDescription.appendText("answer text was ").appendValue(item.getAnswerText());
            return false;
        }
        if (item.getCorrect() != expected.getCorrect()) {
            mismatchDescription.appendText("correct was ").appendValue(item.getCorrect());
            return false;
        }
        if (!Objects.equals(item.getFeedback(), expected.getFeedback())) {
            mismatchDescription.appendText("feedback was ").appendValue(item.getFeedback());
            return false;
        }

        return true;
    }

    @Override
    public void describeTo(final Description description) {
        description.appendText("matches MultipleChoiceAnswerInput");
        description.appendValue(expected);
    }
}
