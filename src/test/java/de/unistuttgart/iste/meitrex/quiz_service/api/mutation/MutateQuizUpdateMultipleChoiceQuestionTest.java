package de.unistuttgart.iste.meitrex.quiz_service.api.mutation;

import de.unistuttgart.iste.meitrex.common.testutil.GraphQlApiTest;
import de.unistuttgart.iste.meitrex.common.testutil.InjectCurrentUserHeader;
import de.unistuttgart.iste.meitrex.common.testutil.TablesToDelete;
import de.unistuttgart.iste.meitrex.common.user_handling.LoggedInUser;
import de.unistuttgart.iste.meitrex.generated.dto.MultipleChoiceAnswerInput;
import de.unistuttgart.iste.meitrex.generated.dto.MultipleChoiceQuestion;
import de.unistuttgart.iste.meitrex.generated.dto.UpdateMultipleChoiceQuestionInput;
import de.unistuttgart.iste.meitrex.quiz_service.TestData;
import de.unistuttgart.iste.meitrex.quiz_service.api.QuizFragments;
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
import static de.unistuttgart.iste.meitrex.quiz_service.TestData.createMultipleChoiceQuestion;
import static de.unistuttgart.iste.meitrex.quiz_service.matcher.MultipleChoiceQuestionDtoToUpdateInputMatcher.matchesInput;
import static de.unistuttgart.iste.meitrex.quiz_service.matcher.MultipleChoiceQuestionEntityToUpdateInputMatcher.matchesUpdateQuizInput;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

@GraphQlApiTest
@TablesToDelete({"multiple_choice_question_answers", "multiple_choice_question", "quiz_question_pool", "question", "quiz"})
class MutateQuizUpdateMultipleChoiceQuestionTest {

    @Autowired
    private QuizRepository quizRepository;
    private final UUID courseId = UUID.randomUUID();

    @InjectCurrentUserHeader
    private final LoggedInUser loggedInUser = userWithMembershipInCourseWithId(courseId, LoggedInUser.UserRoleInCourse.ADMINISTRATOR);

    @Test
    @Transactional
    @Commit
    void testUpdateMultipleChoiceQuestion(final GraphQlTester graphQlTester) {
        QuizEntity quizEntity = TestData.exampleQuizBuilder(courseId)
                .questionPool(List.of(
                        createMultipleChoiceQuestion(1, "what is the capital of Germany?", "Berlin", "Paris")))
                .build();
        quizEntity = quizRepository.save(quizEntity);

        final UpdateMultipleChoiceQuestionInput input = UpdateMultipleChoiceQuestionInput.builder()
                .setId(quizEntity.getQuestionPool().get(0).getId())
                .setText("what is the capital of France?")
                .setAnswers(List.of(
                        MultipleChoiceAnswerInput.builder()
                                .setAnswerText("Paris")
                                .setCorrect(true)
                                .build(),
                        MultipleChoiceAnswerInput.builder()
                                .setAnswerText("Madrid")
                                .setCorrect(false)
                                .build()))
                .build();

        final String query = QuizFragments.FRAGMENT_DEFINITION + """
                mutation($id: UUID!, $input: UpdateMultipleChoiceQuestionInput!) {
                    mutateQuiz(assessmentId: $id) {
                        updateMultipleChoiceQuestion(input: $input) {
                            ...QuizAllFields
                        }
                    }
                }
                """;

        final List<MultipleChoiceQuestion> questions = graphQlTester.document(query)
                .variable("input", input)
                .variable("id", quizEntity.getAssessmentId())
                .execute()
                .path("mutateQuiz.updateMultipleChoiceQuestion.questionPool")
                .entityList(MultipleChoiceQuestion.class)
                .get();

        assertThat(questions, hasSize(1));
        System.out.println(questions.get(0));
        assertThat(questions.get(0), matchesInput(input));

        final QuizEntity updatedQuiz = quizRepository.findById(quizEntity.getAssessmentId()).orElseThrow();
        assertThat(updatedQuiz.getQuestionPool(), hasSize(1));
        assertThat(updatedQuiz.getQuestionPool().get(0), matchesUpdateQuizInput(input));

    }
}
