package de.unistuttgart.iste.meitrex.quiz_service.api.mutation;

import de.unistuttgart.iste.meitrex.quiz_service.api.QuizFragments;
import de.unistuttgart.iste.meitrex.quiz_service.persistence.entity.NumericQuestionEntity;
import de.unistuttgart.iste.meitrex.quiz_service.persistence.entity.QuestionEntity;
import de.unistuttgart.iste.meitrex.quiz_service.persistence.entity.QuizEntity;
import de.unistuttgart.iste.meitrex.common.testutil.GraphQlApiTest;
import de.unistuttgart.iste.meitrex.common.testutil.InjectCurrentUserHeader;
import de.unistuttgart.iste.meitrex.common.testutil.TablesToDelete;
import de.unistuttgart.iste.meitrex.common.user_handling.LoggedInUser;
import de.unistuttgart.iste.meitrex.generated.dto.CreateNumericQuestionInput;
import de.unistuttgart.iste.meitrex.quiz_service.TestData;
import de.unistuttgart.iste.meitrex.quiz_service.persistence.repository.QuizRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.annotation.Commit;
import org.springframework.test.context.ContextConfiguration;
import de.unistuttgart.iste.meitrex.common.testutil.MockTestPublisherConfiguration;

import java.util.List;
import java.util.UUID;

import static de.unistuttgart.iste.meitrex.common.testutil.TestUsers.userWithMembershipInCourseWithId;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

@GraphQlApiTest
@ContextConfiguration(classes = MockTestPublisherConfiguration.class)
class MutateQuizAddNumericQuestionTest {

    @Autowired
    private QuizRepository quizRepository;
    private final UUID courseId = UUID.randomUUID();

    @InjectCurrentUserHeader
    private final LoggedInUser loggedInUser = userWithMembershipInCourseWithId(courseId, LoggedInUser.UserRoleInCourse.ADMINISTRATOR);

    private static final String ADD_NUMERIC_QUESTION_MUTATION = QuizFragments.FRAGMENT_DEFINITION + """
            mutation($id: UUID!, $input: CreateNumericQuestionInput!) {
                mutateQuiz(assessmentId: $id) {
                    _internal_noauth_addNumericQuestion(input: $input) {
                        ...QuizAllFields
                    }
                }
            }
            """;

    /**
     * Given a quiz
     * When the "addNumericQuestion" mutation is called with a new question
     * Then the new question is added to the quiz
     */
    @Test
    @Transactional
    @Commit
    void testAddNumericQuestion(final GraphQlTester graphQlTester) {
        QuizEntity quizEntity = TestData.exampleQuizBuilder(courseId)
                .questionPool(List.of())
                .build();
        quizEntity = quizRepository.save(quizEntity);
        UUID itemId = UUID.randomUUID();
        final CreateNumericQuestionInput input = CreateNumericQuestionInput.builder()
                .setItemId(itemId)
                .setHint("hint")
                .setText("question")
                .setFeedback("feedback")
                .setCorrectAnswer(2.0)
                .setTolerance(0.5)
                .build();

        graphQlTester.document(ADD_NUMERIC_QUESTION_MUTATION)
                .variable("input", input)
                .variable("id", quizEntity.getAssessmentId())
                .execute()
                .path("mutateQuiz._internal_noauth_addNumericQuestion.questionPool[0].number")
                .entity(Integer.class).isEqualTo(1)

                .path("mutateQuiz._internal_noauth_addNumericQuestion.questionPool[0].itemId")
                .entity(UUID.class).isEqualTo(itemId)

                .path("mutateQuiz._internal_noauth_addNumericQuestion.questionPool[0].hint")
                .entity(String.class).isEqualTo("hint")

                .path("mutateQuiz._internal_noauth_addNumericQuestion.questionPool[0].text")
                .entity(String.class).isEqualTo("question")

                .path("mutateQuiz._internal_noauth_addNumericQuestion.questionPool[0].feedback")
                .entity(String.class).isEqualTo("feedback")

                .path("mutateQuiz._internal_noauth_addNumericQuestion.questionPool[0].correctAnswer")
                .entity(Double.class).isEqualTo(2.0)

                .path("mutateQuiz._internal_noauth_addNumericQuestion.questionPool[0].tolerance")
                .entity(Double.class).isEqualTo(0.5);

        final QuestionEntity questionEntity = quizRepository.findById(quizEntity.getAssessmentId())
                .orElseThrow()
                .getQuestionPool()
                .get(0);

        assertThat(questionEntity, instanceOf(NumericQuestionEntity.class));
        final NumericQuestionEntity numericQuestionEntity = (NumericQuestionEntity) questionEntity;

        assertThat(numericQuestionEntity.getItemId(), is(itemId));
        assertThat(numericQuestionEntity.getHint(), is("hint"));
        assertThat(numericQuestionEntity.getText(), is("question"));
        assertThat(numericQuestionEntity.getFeedback(), is("feedback"));
        assertThat(numericQuestionEntity.getCorrectAnswer(), is(2.0));
        assertThat(numericQuestionEntity.getTolerance(), is(0.5));

    }
}
