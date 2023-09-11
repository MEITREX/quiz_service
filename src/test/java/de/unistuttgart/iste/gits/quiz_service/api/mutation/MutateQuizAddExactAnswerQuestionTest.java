package de.unistuttgart.iste.gits.quiz_service.api.mutation;

import de.unistuttgart.iste.gits.common.testutil.GraphQlApiTest;
import de.unistuttgart.iste.gits.common.testutil.TablesToDelete;
import de.unistuttgart.iste.gits.generated.dto.CreateExactAnswerQuestionInput;
import de.unistuttgart.iste.gits.generated.dto.ResourceMarkdownInput;
import de.unistuttgart.iste.gits.quiz_service.TestData;
import de.unistuttgart.iste.gits.quiz_service.api.QuizFragments;
import de.unistuttgart.iste.gits.quiz_service.persistence.entity.*;
import de.unistuttgart.iste.gits.quiz_service.persistence.repository.QuizRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.annotation.Commit;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@GraphQlApiTest
@TablesToDelete({"exact_answer_question_correct_answers", "exact_answer_question", "quiz_question_pool", "question", "quiz"})
class MutateQuizAddExactAnswerQuestionTest {

    @Autowired
    private QuizRepository quizRepository;

    private static final String ADD_EXACT_ANSWER_QUESTION_MUTATION = QuizFragments.FRAGMENT_DEFINITION + """
            mutation($id: UUID!, $input: CreateExactAnswerQuestionInput!) {
                mutateQuiz(assessmentId: $id) {
                    addExactAnswerQuestion(input: $input) {
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
    void testAddExactAnswerQuestion(GraphQlTester graphQlTester) {
        QuizEntity quizEntity = TestData.exampleQuizBuilder()
                .questionPool(List.of())
                .build();
        quizEntity = quizRepository.save(quizEntity);

        CreateExactAnswerQuestionInput input = CreateExactAnswerQuestionInput.builder()
                .setHint(new ResourceMarkdownInput("hint"))
                .setText(new ResourceMarkdownInput("question"))
                .setFeedback(new ResourceMarkdownInput("feedback"))
                .setCaseSensitive(true)
                .setCorrectAnswers(List.of("a", "b"))
                .build();


        graphQlTester.document(ADD_EXACT_ANSWER_QUESTION_MUTATION)
                .variable("input", input)
                .variable("id", quizEntity.getAssessmentId())
                .execute()
                .path("mutateQuiz.addExactAnswerQuestion.questionPool[0].number")
                .entity(Integer.class).isEqualTo(1)

                .path("mutateQuiz.addExactAnswerQuestion.questionPool[0].hint.text")
                .entity(String.class).isEqualTo("hint")

                .path("mutateQuiz.addExactAnswerQuestion.questionPool[0].text.text")
                .entity(String.class).isEqualTo("question")

                .path("mutateQuiz.addExactAnswerQuestion.questionPool[0].feedback.text")
                .entity(String.class).isEqualTo("feedback")

                .path("mutateQuiz.addExactAnswerQuestion.questionPool[0].caseSensitive")
                .entity(Boolean.class).isEqualTo(true)

                .path("mutateQuiz.addExactAnswerQuestion.questionPool[0].correctAnswers")
                .entityList(String.class).contains("a", "b");

        QuestionEntity questionEntity = quizRepository.findById(quizEntity.getAssessmentId())
                .orElseThrow()
                .getQuestionPool()
                .get(0);

        assertThat(questionEntity, instanceOf(ExactAnswerQuestionEntity.class));
        ExactAnswerQuestionEntity exactAnswerQuestionEntity = (ExactAnswerQuestionEntity) questionEntity;

        assertThat(exactAnswerQuestionEntity.getHint().getText(), is("hint"));
        assertThat(exactAnswerQuestionEntity.getText().getText(), is("question"));
        assertThat(exactAnswerQuestionEntity.getFeedback().getText(), is("feedback"));
        assertThat(exactAnswerQuestionEntity.isCaseSensitive(), is(true));
        assertThat(exactAnswerQuestionEntity.getCorrectAnswers(), hasSize(2));
        assertThat(exactAnswerQuestionEntity.getCorrectAnswers(), containsInAnyOrder("a", "b"));

    }
}