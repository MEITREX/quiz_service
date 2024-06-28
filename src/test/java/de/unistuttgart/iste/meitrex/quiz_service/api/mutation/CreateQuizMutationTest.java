package de.unistuttgart.iste.meitrex.quiz_service.api.mutation;

import de.unistuttgart.iste.meitrex.common.testutil.GraphQlApiTest;
import de.unistuttgart.iste.meitrex.common.testutil.TablesToDelete;
import de.unistuttgart.iste.meitrex.generated.dto.CreateQuizInput;
import de.unistuttgart.iste.meitrex.generated.dto.MultipleChoiceQuestion;
import de.unistuttgart.iste.meitrex.generated.dto.QuestionPoolingMode;
import de.unistuttgart.iste.meitrex.quiz_service.api.QuizFragments;
import de.unistuttgart.iste.meitrex.quiz_service.persistence.repository.QuizRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.annotation.Commit;

import java.util.List;
import java.util.UUID;

import static de.unistuttgart.iste.meitrex.quiz_service.matcher.QuizEntityToCreateInputMatcher.matchesCreateQuizInput;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

@GraphQlApiTest
@TablesToDelete({"multiple_choice_question_answers", "multiple_choice_question", "quiz_question_pool", "question", "quiz"})
class CreateQuizMutationTest {

    @Autowired
    private QuizRepository quizRepository;

    /**
     * Given a quiz with no questions
     * When the "createQuiz" mutation is executed
     * Then the quiz is created
     */
    @Test
    @Transactional
    @Commit
    void testCreateQuizWithoutQuestions(final GraphQlTester graphQlTester) {
        final UUID assessmentId = UUID.randomUUID();
        final UUID courseId = UUID.randomUUID();
        final CreateQuizInput createQuizInput = CreateQuizInput.builder()
                .setRequiredCorrectAnswers(1)
                .setQuestionPoolingMode(QuestionPoolingMode.RANDOM)
                .setNumberOfRandomlySelectedQuestions(2)
                .build();

        final String query = QuizFragments.FRAGMENT_DEFINITION + """
                mutation createQuiz($courseId: UUID!, $id: UUID!, $input: CreateQuizInput!) {
                    createQuiz: _internal_noauth_createQuiz(courseId: $courseId, assessmentId: $id, input: $input) {
                        ...QuizAllFields
                    }
                }""";

        // note that deserialization of the result into Quiz dto is not possible because "Question" is an interface
        // so check the fields manually
        final List<MultipleChoiceQuestion> questions = graphQlTester.document(query)
                .variable("input", createQuizInput)
                .variable("courseId", courseId)
                .variable("id", assessmentId)
                .execute()

                .path("createQuiz.assessmentId").entity(UUID.class)
                .isEqualTo(assessmentId)

                .path("createQuiz.requiredCorrectAnswers").entity(Integer.class)
                .isEqualTo(1)

                .path("createQuiz.questionPoolingMode").entity(QuestionPoolingMode.class)
                .isEqualTo(QuestionPoolingMode.RANDOM)

                .path("createQuiz.numberOfRandomlySelectedQuestions").entity(Integer.class)
                .isEqualTo(2)

                .path("createQuiz.questionPool")
                .entityList(MultipleChoiceQuestion.class)
                .get();

        assertThat(questions, is(empty()));

        assertThat(quizRepository.count(), is(1L));
        assertThat(quizRepository.findAll().get(0), matchesCreateQuizInput(createQuizInput));
        assertThat(quizRepository.findAll().get(0).getCourseId(), is(courseId));
    }
}
