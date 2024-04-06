package de.unistuttgart.iste.gits.quiz_service.api.mutation;

import de.unistuttgart.iste.gits.quiz_service.TestData;
import de.unistuttgart.iste.gits.quiz_service.api.QuizFragments;
import de.unistuttgart.iste.gits.quiz_service.persistence.entity.NumericQuestionEntity;
import de.unistuttgart.iste.gits.quiz_service.persistence.entity.QuizEntity;
import de.unistuttgart.iste.gits.quiz_service.persistence.repository.QuizRepository;
import de.unistuttgart.iste.meitrex.common.testutil.GraphQlApiTest;
import de.unistuttgart.iste.meitrex.common.testutil.InjectCurrentUserHeader;
import de.unistuttgart.iste.meitrex.common.testutil.TablesToDelete;
import de.unistuttgart.iste.meitrex.common.user_handling.LoggedInUser;
import de.unistuttgart.iste.meitrex.generated.dto.UpdateNumericQuestionInput;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.annotation.Commit;

import java.util.List;
import java.util.UUID;

import static de.unistuttgart.iste.meitrex.common.testutil.TestUsers.userWithMembershipInCourseWithId;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

@GraphQlApiTest
@TablesToDelete({"numeric_question", "quiz_question_pool", "question", "quiz"})
class MutateQuizUpdateNumericQuestionTest {

    @Autowired
    private QuizRepository quizRepository;
    private final UUID courseId = UUID.randomUUID();

    @InjectCurrentUserHeader
    private final LoggedInUser loggedInUser = userWithMembershipInCourseWithId(courseId, LoggedInUser.UserRoleInCourse.ADMINISTRATOR);

    @Test
    @Transactional
    @Commit
    void testUpdateNumericQuestion(final GraphQlTester graphQlTester) {
        QuizEntity quizEntity = TestData.exampleQuizBuilder(courseId)
                .questionPool(List.of(
                        TestData.createNumericQuestion(1, "question", 2.0)))
                .build();
        quizEntity = quizRepository.save(quizEntity);

        final UpdateNumericQuestionInput input = UpdateNumericQuestionInput.builder()
                .setItemId(quizEntity.getQuestionPool().get(0).getItemId())
                .setHint("new hint")
                .setText("new question")
                .setCorrectAnswer(3.0)
                .setTolerance(0.1)
                .setFeedback("new feedback")
                .build();

        final String query = QuizFragments.FRAGMENT_DEFINITION + """
                mutation($id: UUID!, $input: UpdateNumericQuestionInput!) {
                    mutateQuiz(assessmentId: $id) {
                        updateNumericQuestion(input: $input) {
                            ...QuizAllFields
                        }
                    }
                }
                """;

        graphQlTester.document(query)
                .variable("id", quizEntity.getAssessmentId())
                .variable("input", input)
                .execute()
                .path("mutateQuiz.updateNumericQuestion.questionPool[0].number").entity(Integer.class).isEqualTo(1)
                .path("mutateQuiz.updateNumericQuestion.questionPool[0].text").entity(String.class).isEqualTo("new question")
                .path("mutateQuiz.updateNumericQuestion.questionPool[0].hint").entity(String.class).isEqualTo("new hint")
                .path("mutateQuiz.updateNumericQuestion.questionPool[0].correctAnswer").entity(Double.class).isEqualTo(3.0)
                .path("mutateQuiz.updateNumericQuestion.questionPool[0].tolerance").entity(Double.class).isEqualTo(0.1);

        final QuizEntity updatedQuiz = quizRepository.findById(quizEntity.getAssessmentId()).orElseThrow();
        assertThat(updatedQuiz.getQuestionPool(), hasSize(1));
        final NumericQuestionEntity updatedQuestion = (NumericQuestionEntity) updatedQuiz.getQuestionPool().get(0);
        assertThat(updatedQuestion.getText(), is("new question"));
        assertThat(updatedQuestion.getHint(), is("new hint"));
        assertThat(updatedQuestion.getCorrectAnswer(), is(3.0));
        assertThat(updatedQuestion.getTolerance(), is(0.1));
        assertThat(updatedQuestion.getFeedback(), is("new feedback"));
    }
}
