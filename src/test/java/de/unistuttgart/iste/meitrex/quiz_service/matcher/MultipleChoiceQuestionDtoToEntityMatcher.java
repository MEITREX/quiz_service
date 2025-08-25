package de.unistuttgart.iste.meitrex.quiz_service.matcher;

import de.unistuttgart.iste.meitrex.quiz_service.persistence.entity.MultipleChoiceQuestionEntity;
import de.unistuttgart.iste.meitrex.quiz_service.persistence.entity.QuestionEntity;
import de.unistuttgart.iste.meitrex.generated.dto.MultipleChoiceQuestion;
import de.unistuttgart.iste.meitrex.generated.dto.QuestionType;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;

import java.util.Objects;

/**
 * Matcher for comparing a {@link MultipleChoiceQuestionEntity} to a {@link MultipleChoiceQuestionEntity}.
 */
public class MultipleChoiceQuestionDtoToEntityMatcher extends TypeSafeDiagnosingMatcher<MultipleChoiceQuestion> {

    private final QuestionEntity expected;

    public MultipleChoiceQuestionDtoToEntityMatcher(final QuestionEntity expected) {
        this.expected = expected;
    }

    public static MultipleChoiceQuestionDtoToEntityMatcher matchesEntity(final QuestionEntity expected) {
        return new MultipleChoiceQuestionDtoToEntityMatcher(expected);
    }

    @Override
    protected boolean matchesSafely(final MultipleChoiceQuestion item, final Description mismatchDescription) {
        if (!(expected instanceof final MultipleChoiceQuestionEntity multipleChoiceQuestionEntity)) {
            mismatchDescription.appendText("expected was not a MultipleChoiceQuestionEntity");
            return false;
        }
        // for convenience, we do not check the id as it is usually null before saving
        if (!Objects.equals(item.getItemId(), expected.getItemId())) {
            mismatchDescription.appendText("id was ").appendValue(item.getItemId());
            return false;
        }

        if (item.getNumber() != expected.getNumber()) {
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
        if (!Objects.equals(item.getText(), multipleChoiceQuestionEntity.getText())) {
            mismatchDescription.appendText("text was ").appendValue(item.getText());
            return false;
        }
        if (item.getAnswers().size() != multipleChoiceQuestionEntity.getAnswers().size()) {
            mismatchDescription.appendText("answers size was ").appendValue(item.getAnswers().size());
            return false;
        }
        for (int i = 0; i < item.getAnswers().size(); i++) {
            if (!MultipleChoiceAnswerDtoToEntityMatcher
                    .matchesEntity(multipleChoiceQuestionEntity.getAnswers().get(i))
                    .matchesSafely(item.getAnswers().get(i), mismatchDescription)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public void describeTo(final Description description) {
        description.appendText("matches QuestionEntity");
        description.appendValue(expected);
    }
}
