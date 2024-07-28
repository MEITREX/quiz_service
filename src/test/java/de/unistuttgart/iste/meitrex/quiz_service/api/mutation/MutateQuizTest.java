package de.unistuttgart.iste.meitrex.quiz_service.api.mutation;

import de.unistuttgart.iste.meitrex.quiz_service.TestData;
import de.unistuttgart.iste.meitrex.quiz_service.persistence.entity.QuizEntity;
import de.unistuttgart.iste.meitrex.quiz_service.persistence.repository.QuizRepository;

import de.unistuttgart.iste.meitrex.common.testutil.GraphQlApiTest;
import de.unistuttgart.iste.meitrex.common.testutil.InjectCurrentUserHeader;
import de.unistuttgart.iste.meitrex.common.testutil.TablesToDelete;
import de.unistuttgart.iste.meitrex.common.user_handling.LoggedInUser;
import de.unistuttgart.iste.meitrex.generated.dto.QuestionPoolingMode;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.test.tester.GraphQlTester;

import java.util.List;
import java.util.UUID;

import static de.unistuttgart.iste.meitrex.common.testutil.TestUsers.userWithMembershipInCourseWithId;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@GraphQlApiTest
class MutateQuizTest {

    @Autowired
    private QuizRepository quizRepository;
    private final UUID courseId = UUID.randomUUID();

    @InjectCurrentUserHeader
    private final LoggedInUser loggedInUser = userWithMembershipInCourseWithId(courseId, LoggedInUser.UserRoleInCourse.ADMINISTRATOR);

    /**
     * Given a quiz
     * When the "mutateQuiz" mutation is called with the quiz's assessment id and some nested mutations
     * Then the quiz is updated accordingly
     */
    @Test
    void testMutateQuiz(final GraphQlTester graphQlTester) {
        QuizEntity quizEntity = QuizEntity.builder()
                .courseId(courseId)
                .assessmentId(UUID.randomUUID())
                .questionPoolingMode(QuestionPoolingMode.RANDOM)
                .numberOfRandomlySelectedQuestions(1)
                .requiredCorrectAnswers(1)
                .questionPool(List.of(
                        TestData.createMultipleChoiceQuestion(1, "what is the capital of Germany?", "Berlin", "Paris")))
                .build();
        quizEntity = quizRepository.save(quizEntity);

        final String query = """
                mutation($id: UUID!) {
                    mutateQuiz(assessmentId: $id) {
                        setQuestionPoolingMode(questionPoolingMode: ORDERED) { assessmentId }
                        setNumberOfRandomlySelectedQuestions(numberOfRandomlySelectedQuestions: 2) { assessmentId }
                        result: setRequiredCorrectAnswers(requiredCorrectAnswers: 2) {
                            assessmentId
                            requiredCorrectAnswers
                            questionPoolingMode
                            numberOfRandomlySelectedQuestions
                        }
                    }
                }
                """;

        graphQlTester.document(query)
                .variable("id", quizEntity.getAssessmentId())
                .execute()
                .path("mutateQuiz.result.assessmentId").entity(UUID.class).isEqualTo(quizEntity.getAssessmentId())

                .path("mutateQuiz.result.requiredCorrectAnswers").entity(Integer.class)
                .isEqualTo(2)

                .path("mutateQuiz.result.questionPoolingMode").entity(QuestionPoolingMode.class)
                .isEqualTo(QuestionPoolingMode.ORDERED)

                .path("mutateQuiz.result.numberOfRandomlySelectedQuestions").entity(Integer.class)
                .isEqualTo(2);

        final QuizEntity updatedQuiz = quizRepository.findById(quizEntity.getAssessmentId()).orElseThrow();
        assertThat(updatedQuiz.getRequiredCorrectAnswers(), is(2));
        assertThat(updatedQuiz.getQuestionPoolingMode(), is(QuestionPoolingMode.ORDERED));
        assertThat(updatedQuiz.getNumberOfRandomlySelectedQuestions(), is(2));
    }
}
