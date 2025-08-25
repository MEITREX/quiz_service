package de.unistuttgart.iste.meitrex.quiz_service.matcher;


import de.unistuttgart.iste.meitrex.generated.dto.*;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;

import java.util.Objects;

/**
 * Matcher for comparing a {@link MultipleChoiceQuestion} to a {@link CreateMultipleChoiceQuestionInput}.
 */
public class MultipleChoiceQuestionDtoToCreateInputMatcher extends TypeSafeDiagnosingMatcher<MultipleChoiceQuestion> {

    private final CreateMultipleChoiceQuestionInput expected;

    public MultipleChoiceQuestionDtoToCreateInputMatcher(final CreateMultipleChoiceQuestionInput expected) {
        this.expected = expected;
    }

    public static MultipleChoiceQuestionDtoToCreateInputMatcher matchesInput(final CreateMultipleChoiceQuestionInput expected) {
        return new MultipleChoiceQuestionDtoToCreateInputMatcher(expected);
    }

    @Override
    protected boolean matchesSafely(final MultipleChoiceQuestion item, final Description mismatchDescription) {
        if (!Objects.equals(item.getItemId(), expected.getItemId())) {
            mismatchDescription.appendText("id was ").appendValue(item.getItemId());
            return false;
        }
        if (!Objects.equals(item.getNumber(), expected.getNumber())) {
            mismatchDescription.appendText("number was ").appendValue(item.getNumber());
            return false;
        }
        if (!Objects.equals(item.getType(), QuestionType.MULTIPLE_CHOICE)) {
            mismatchDescription.appendText("type was ").appendValue(item.getType());
            return false;
        }
        if (!Objects.equals(item.getHint(), expected.getHint())) {
            mismatchDescription.appendText("hint was ").appendValue(item.getHint());
            return false;
        }
        if (!Objects.equals(item.getText(), expected.getText())) {
            mismatchDescription.appendText("text was ").appendValue(item.getText());
            return false;
        }
        if (item.getAnswers().size() != expected.getAnswers().size()) {
            mismatchDescription.appendText("answers size was ").appendValue(item.getAnswers().size());
            return false;
        }
        for (int i = 0; i < item.getAnswers().size(); i++) {
            if (!MultipleChoiceAnswerDtoToInputMatcher.matchesInput(expected.getAnswers().get(i))
                    .matchesSafely(item.getAnswers().get(i), mismatchDescription)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public void describeTo(final Description description) {
        description.appendText("matches CreateMultipleChoiceQuestionInput");
        description.appendValue(expected);
    }
}
