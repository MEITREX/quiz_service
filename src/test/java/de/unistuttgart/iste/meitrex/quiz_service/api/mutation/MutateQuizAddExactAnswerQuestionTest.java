package de.unistuttgart.iste.meitrex.quiz_service.api.mutation;

import de.unistuttgart.iste.meitrex.quiz_service.api.QuizFragments;
import de.unistuttgart.iste.meitrex.quiz_service.persistence.entity.ExactAnswerQuestionEntity;
import de.unistuttgart.iste.meitrex.quiz_service.persistence.entity.QuestionEntity;
import de.unistuttgart.iste.meitrex.quiz_service.persistence.entity.QuizEntity;

import de.unistuttgart.iste.meitrex.common.testutil.GraphQlApiTest;
import de.unistuttgart.iste.meitrex.common.testutil.InjectCurrentUserHeader;
import de.unistuttgart.iste.meitrex.common.testutil.TablesToDelete;
import de.unistuttgart.iste.meitrex.common.user_handling.LoggedInUser;
import de.unistuttgart.iste.meitrex.generated.dto.CreateExactAnswerQuestionInput;
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
import static org.hamcrest.Matchers.*;

import static de.unistuttgart.iste.meitrex.common.testutil.TestUsers.userWithMembershipInCourseWithId;



@GraphQlApiTest
@ContextConfiguration(classes = MockTestPublisherConfiguration.class)
class MutateQuizAddExactAnswerQuestionTest {

    @Autowired
    private QuizRepository quizRepository;
    private final UUID courseId = UUID.randomUUID();

    @InjectCurrentUserHeader
    private final LoggedInUser loggedInUser = userWithMembershipInCourseWithId(courseId, LoggedInUser.UserRoleInCourse.ADMINISTRATOR);

    private static final String ADD_EXACT_ANSWER_QUESTION_MUTATION = QuizFragments.FRAGMENT_DEFINITION + """
            mutation($id: UUID!, $input: CreateExactAnswerQuestionInput!) {
                mutateQuiz(assessmentId: $id) {
                    _internal_noauth_addExactAnswerQuestion(input: $input) {
                        ...QuizAllFields
                    }
                }
            }
            """;

    /**
     * Given a quiz
     * When the "addExactAnswer" mutation is called with a new question
     * Then the new question is added to the quiz
     */
    @Test
    @Transactional
    @Commit
    void testAddExactAnswerQuestion(final GraphQlTester graphQlTester) {
        QuizEntity quizEntity = TestData.exampleQuizBuilder(courseId)
                .questionPool(List.of())
                .build();
        quizEntity = quizRepository.save(quizEntity);
        UUID itemId = UUID.randomUUID();
        final CreateExactAnswerQuestionInput input = CreateExactAnswerQuestionInput.builder()
                .setItemId(itemId)
                .setHint("hint")
                .setText("question")
                .setFeedback("feedback")
                .setCaseSensitive(true)
                .setCorrectAnswers(List.of("a", "b"))
                .build();


        graphQlTester.document(ADD_EXACT_ANSWER_QUESTION_MUTATION)
                .variable("input", input)
                .variable("id", quizEntity.getAssessmentId())
                .execute()
                .path("mutateQuiz._internal_noauth_addExactAnswerQuestion.questionPool[0].number")
                .entity(Integer.class).isEqualTo(1)

                .path("mutateQuiz._internal_noauth_addExactAnswerQuestion.questionPool[0].itemId")
                .entity(UUID.class).isEqualTo(itemId)

                .path("mutateQuiz._internal_noauth_addExactAnswerQuestion.questionPool[0].hint")
                .entity(String.class).isEqualTo("hint")

                .path("mutateQuiz._internal_noauth_addExactAnswerQuestion.questionPool[0].text")
                .entity(String.class).isEqualTo("question")

                .path("mutateQuiz._internal_noauth_addExactAnswerQuestion.questionPool[0].feedback")
                .entity(String.class).isEqualTo("feedback")

                .path("mutateQuiz._internal_noauth_addExactAnswerQuestion.questionPool[0].caseSensitive")
                .entity(Boolean.class).isEqualTo(true)

                .path("mutateQuiz._internal_noauth_addExactAnswerQuestion.questionPool[0].correctAnswers")
                .entityList(String.class).contains("a", "b");

        final QuestionEntity questionEntity = quizRepository.findById(quizEntity.getAssessmentId())
                .orElseThrow()
                .getQuestionPool()
                .get(0);

        assertThat(questionEntity, instanceOf(ExactAnswerQuestionEntity.class));
        final ExactAnswerQuestionEntity exactAnswerQuestionEntity = (ExactAnswerQuestionEntity) questionEntity;
        assertThat(exactAnswerQuestionEntity.getItemId(), is(itemId));
        assertThat(exactAnswerQuestionEntity.getHint(), is("hint"));
        assertThat(exactAnswerQuestionEntity.getText(), is("question"));
        assertThat(exactAnswerQuestionEntity.getFeedback(), is("feedback"));
        assertThat(exactAnswerQuestionEntity.isCaseSensitive(), is(true));
        assertThat(exactAnswerQuestionEntity.getCorrectAnswers(), hasSize(2));
        assertThat(exactAnswerQuestionEntity.getCorrectAnswers(), containsInAnyOrder("a", "b"));

    }
}
