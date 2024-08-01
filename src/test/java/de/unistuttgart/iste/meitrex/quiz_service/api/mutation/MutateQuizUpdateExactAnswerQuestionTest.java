package de.unistuttgart.iste.meitrex.quiz_service.api.mutation;

import de.unistuttgart.iste.meitrex.quiz_service.api.QuizFragments;
import de.unistuttgart.iste.meitrex.common.testutil.GraphQlApiTest;
import de.unistuttgart.iste.meitrex.common.testutil.InjectCurrentUserHeader;
import de.unistuttgart.iste.meitrex.common.testutil.TablesToDelete;
import de.unistuttgart.iste.meitrex.common.user_handling.LoggedInUser;
import de.unistuttgart.iste.meitrex.generated.dto.UpdateExactAnswerQuestionInput;
import de.unistuttgart.iste.meitrex.quiz_service.TestData;
import de.unistuttgart.iste.meitrex.quiz_service.persistence.entity.ExactAnswerQuestionEntity;
import de.unistuttgart.iste.meitrex.quiz_service.persistence.entity.QuizEntity;
import de.unistuttgart.iste.meitrex.quiz_service.persistence.repository.QuizRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.annotation.Commit;

import java.util.List;
import java.util.UUID;

import static de.unistuttgart.iste.meitrex.common.testutil.TestUsers.userWithMembershipInCourseWithId;
import static de.unistuttgart.iste.meitrex.quiz_service.TestData.createExactAnswerQuestion;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@GraphQlApiTest
class MutateQuizUpdateExactAnswerQuestionTest {

    @Autowired
    private QuizRepository quizRepository;
    private final UUID courseId = UUID.randomUUID();

    @InjectCurrentUserHeader
    private final LoggedInUser loggedInUser = userWithMembershipInCourseWithId(courseId, LoggedInUser.UserRoleInCourse.ADMINISTRATOR);

    @Test
    @Transactional
    @Commit
    void testUpdateExactAnswerQuestion(final GraphQlTester graphQlTester) {
        QuizEntity quizEntity = TestData.exampleQuizBuilder(courseId)
                .questionPool(List.of(
                        createExactAnswerQuestion(1, "question", "answer")))
                .build();
        quizEntity = quizRepository.save(quizEntity);

        final UpdateExactAnswerQuestionInput input = UpdateExactAnswerQuestionInput.builder()
                .setItemId(quizEntity.getQuestionPool().get(0).getItemId())
                .setHint("new hint")
                .setText("new question")
                .setFeedback("new feedback")
                .setCaseSensitive(false)
                .setCorrectAnswers(List.of("newA", "newB"))
                .build();

        final String query = QuizFragments.FRAGMENT_DEFINITION + """
                mutation($id: UUID!, $input: UpdateExactAnswerQuestionInput!) {
                    mutateQuiz(assessmentId: $id) {
                        _internal_noauth_updateExactAnswerQuestion(input: $input) {
                            ...QuizAllFields
                        }
                    }
                }
                """;

        graphQlTester.document(query)
                .variable("id", quizEntity.getAssessmentId())
                .variable("input", input)
                .execute()
                .path("mutateQuiz._internal_noauth_updateExactAnswerQuestion.questionPool[0].number").entity(Integer.class).isEqualTo(1)
                .path("mutateQuiz._internal_noauth_updateExactAnswerQuestion.questionPool[0].text").entity(String.class).isEqualTo("new question")
                .path("mutateQuiz._internal_noauth_updateExactAnswerQuestion.questionPool[0].hint").entity(String.class).isEqualTo("new hint")
                .path("mutateQuiz._internal_noauth_updateExactAnswerQuestion.questionPool[0].feedback").entity(String.class).isEqualTo("new feedback")
                .path("mutateQuiz._internal_noauth_updateExactAnswerQuestion.questionPool[0].caseSensitive").entity(Boolean.class).isEqualTo(false)
                .path("mutateQuiz._internal_noauth_updateExactAnswerQuestion.questionPool[0].correctAnswers").entityList(String.class).contains("newA", "newB");

        final QuizEntity updatedQuiz = quizRepository.findById(quizEntity.getAssessmentId()).orElseThrow();
        assertThat(updatedQuiz.getQuestionPool(), hasSize(1));
        final ExactAnswerQuestionEntity updatedQuestion = (ExactAnswerQuestionEntity) updatedQuiz.getQuestionPool().get(0);
        assertThat(updatedQuestion.getText(), is("new question"));
        assertThat(updatedQuestion.getHint(), is("new hint"));
        assertThat(updatedQuestion.getFeedback(), is("new feedback"));
        assertThat(updatedQuestion.isCaseSensitive(), is(false));
        assertThat(updatedQuestion.getCorrectAnswers(), containsInAnyOrder("newA", "newB"));
    }
}
