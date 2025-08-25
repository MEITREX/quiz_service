package de.unistuttgart.iste.meitrex.quiz_service.api.mutation;

import de.unistuttgart.iste.meitrex.common.testutil.GraphQlApiTest;
import de.unistuttgart.iste.meitrex.common.testutil.InjectCurrentUserHeader;
import de.unistuttgart.iste.meitrex.common.testutil.TablesToDelete;
import de.unistuttgart.iste.meitrex.common.user_handling.LoggedInUser;
import de.unistuttgart.iste.meitrex.generated.dto.*;
import de.unistuttgart.iste.meitrex.quiz_service.TestData;
import de.unistuttgart.iste.meitrex.quiz_service.api.QuizFragments;
import de.unistuttgart.iste.meitrex.quiz_service.persistence.entity.QuizEntity;
import de.unistuttgart.iste.meitrex.quiz_service.persistence.repository.QuizRepository;

import graphql.ErrorType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.ContextConfiguration;
import de.unistuttgart.iste.meitrex.common.testutil.MockTestPublisherConfiguration;

import java.util.List;
import java.util.UUID;

import static de.unistuttgart.iste.meitrex.common.testutil.TestUsers.userWithMembershipInCourseWithId;
import static de.unistuttgart.iste.meitrex.quiz_service.TestData.createMultipleChoiceQuestion;
import static de.unistuttgart.iste.meitrex.quiz_service.matcher.MultipleChoiceQuestionDtoToCreateInputMatcher.matchesInput;
import static de.unistuttgart.iste.meitrex.quiz_service.matcher.MultipleChoiceQuestionDtoToEntityMatcher.matchesEntity;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static de.unistuttgart.iste.meitrex.common.testutil.TestUsers.userWithMembershipInCourseWithId;


@GraphQlApiTest
@ContextConfiguration(classes = MockTestPublisherConfiguration.class)
class MutateQuizAddMultipleChoiceQuestionTest {

    private static final String UPDATE_MULTIPLE_CHOICE_QUESTION_MUTATION = QuizFragments.FRAGMENT_DEFINITION + """
            mutation($id: UUID!, $input: CreateMultipleChoiceQuestionInput!) {
                mutateQuiz(assessmentId: $id) {
                    _internal_noauth_addMultipleChoiceQuestion(input: $input) {
                        ...QuizAllFields
                    }
                }
            }
            """;

    @Autowired
    private QuizRepository quizRepository;
    private final UUID courseId = UUID.randomUUID();

    @InjectCurrentUserHeader
    private final LoggedInUser loggedInUser = userWithMembershipInCourseWithId(courseId, LoggedInUser.UserRoleInCourse.ADMINISTRATOR);

    /**
     * Given a quiz
     * When the "addMultipleChoiceQuestion" mutation is called with a new question
     * Then the new question is added to the quiz
     */
    @Test
    void testAddMultipleChoiceQuestion(final GraphQlTester graphQlTester) {
        QuizEntity quizEntity = TestData.exampleQuizBuilder(courseId)
                .questionPool(List.of(
                        createMultipleChoiceQuestion(1, "what is the capital of Germany?", "Berlin", "Paris")))
                .build();
        quizEntity = quizRepository.save(quizEntity);
        UUID itemId = UUID.randomUUID();
        final CreateMultipleChoiceQuestionInput input = CreateMultipleChoiceQuestionInput.builder()
                .setItemId(itemId)
                .setText("what is the capital of France?")
                .setNumber(2)
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

        final List<MultipleChoiceQuestion> questions = graphQlTester.document(UPDATE_MULTIPLE_CHOICE_QUESTION_MUTATION)
                .variable("input", input)
                .variable("id", quizEntity.getAssessmentId())
                .execute()
                .path("mutateQuiz._internal_noauth_addMultipleChoiceQuestion.questionPool")
                .entityList(MultipleChoiceQuestion.class)
                .get();

        assertThat(questions, hasSize(2));
        assertThat(questions.get(0), matchesEntity(quizEntity.getQuestionPool().get(0)));
        assertThat(questions.get(1), matchesInput(input));
    }

    /**
     * Given a quiz
     * When the "addMultipleChoiceQuestion" mutation is called without a number
     * Then the new question is added to the quiz with the next number
     */
    @Test
    void testAddMultipleChoiceQuestionNextNumber(final GraphQlTester graphQlTester) {
        QuizEntity quizEntity = TestData.exampleQuizBuilder(courseId)
                .questionPool(List.of(
                        createMultipleChoiceQuestion(1, "what is the capital of Germany?", "Berlin", "Paris")))
                .build();
        quizEntity = quizRepository.save(quizEntity);
        UUID itemId = UUID.randomUUID();
        final CreateMultipleChoiceQuestionInput input = CreateMultipleChoiceQuestionInput.builder()
                .setItemId(itemId)
                .setText("what is the capital of France?")
                .setNumber(null) // number should be assigned automatically
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

        final List<MultipleChoiceQuestion> questions = graphQlTester.document(UPDATE_MULTIPLE_CHOICE_QUESTION_MUTATION)
                .variable("input", input)
                .variable("id", quizEntity.getAssessmentId())
                .execute()
                .path("mutateQuiz._internal_noauth_addMultipleChoiceQuestion.questionPool")
                .entityList(MultipleChoiceQuestion.class)
                .get();

        assertThat(questions, hasSize(2));
        assertThat(questions.get(0), matchesEntity(quizEntity.getQuestionPool().get(0)));

        input.setNumber(2); // number should be assigned automatically
        assertThat(questions.get(1), matchesInput(input));
    }

    /**
     * Given a quiz
     * When the "addMultipleChoiceQuestion" mutation is called with an already existing number
     * Then an error is returned
     */
    @Test
    void testAddMultipleChoiceQuestionDuplicateNumber(final GraphQlTester graphQlTester) {
        QuizEntity quizEntity = TestData.exampleQuizBuilder(courseId)
                .questionPool(List.of(
                        createMultipleChoiceQuestion(1, "what is the capital of Germany?", "Berlin", "Paris")))
                .build();
        quizEntity = quizRepository.save(quizEntity);
        UUID itemId = UUID.randomUUID();
        final CreateMultipleChoiceQuestionInput input = CreateMultipleChoiceQuestionInput.builder()
                .setItemId(itemId)
                .setText("what is the capital of France?")
                .setNumber(1) // already existing number
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

        graphQlTester.document(UPDATE_MULTIPLE_CHOICE_QUESTION_MUTATION)
                .variable("input", input)
                .variable("id", quizEntity.getAssessmentId())
                .execute()
                .errors()
                .satisfy(errors -> {
                    assertThat(errors, hasSize(1));
                    assertThat(errors.get(0).getErrorType(), is(ErrorType.ValidationError));
                    assertThat(errors.get(0).getMessage(),
                            containsString("Question number must be unique, but the number 1 is already used"));
                });
    }

    /**
     * Given a quiz
     * When the "addMultipleChoiceQuestion" mutation is called with no correct answer
     * Then an error is returned
     */
    @Test
    void testAddMultipleChoiceQuestionNoCorrectAnswer(final GraphQlTester graphQlTester) {
        QuizEntity quizEntity = TestData.exampleQuizBuilder(courseId)
                .questionPool(List.of(
                        createMultipleChoiceQuestion(1, "what is the capital of Germany?", "Berlin", "Paris")))
                .build();
        quizEntity = quizRepository.save(quizEntity);
        UUID itemId = UUID.randomUUID();
        final CreateMultipleChoiceQuestionInput input = CreateMultipleChoiceQuestionInput.builder()
                .setItemId(itemId)
                .setText("what is the capital of France?")
                .setAnswers(List.of(
                        MultipleChoiceAnswerInput.builder()
                                .setAnswerText("Berlin")
                                .setCorrect(false)
                                .build(),
                        MultipleChoiceAnswerInput.builder()
                                .setAnswerText("Madrid")
                                .setCorrect(false)
                                .build()))
                .build();

        graphQlTester.document(UPDATE_MULTIPLE_CHOICE_QUESTION_MUTATION)
                .variable("input", input)
                .variable("id", quizEntity.getAssessmentId())
                .execute()
                .errors()
                .satisfy(errors -> {
                    assertThat(errors, hasSize(1));
                    assertThat(errors.get(0).getErrorType(), is(ErrorType.ValidationError));
                    assertThat(errors.get(0).getMessage(),
                            containsString("At least one answer must be correct"));
                });
    }

    /**
     * Given a quiz
     * When the "addMultipleChoiceQuestion" mutation is called with only one answer
     * Then an error is returned
     */
    @Test
    void testAddMultipleChoiceQuestionTooFewAnswers(final GraphQlTester graphQlTester) {
        QuizEntity quizEntity = TestData.exampleQuizBuilder(courseId)
                .questionPool(List.of(
                        createMultipleChoiceQuestion(1, "what is the capital of Germany?", "Berlin", "Paris")))
                .build();
        quizEntity = quizRepository.save(quizEntity);
        UUID itemId = UUID.randomUUID();
        final CreateMultipleChoiceQuestionInput input = CreateMultipleChoiceQuestionInput.builder()
                .setItemId(itemId)
                .setText("what is the capital of France?")
                .setAnswers(List.of(
                        MultipleChoiceAnswerInput.builder()
                                .setAnswerText("Paris")
                                .setCorrect(true)
                                .build()))
                .build();

        graphQlTester.document(UPDATE_MULTIPLE_CHOICE_QUESTION_MUTATION)
                .variable("input", input)
                .variable("id", quizEntity.getAssessmentId())
                .execute()
                .errors()
                .satisfy(errors -> {
                    assertThat(errors, hasSize(1));
                    assertThat(errors.get(0).getMessage(), containsString("size must be between 2"));
                });
    }
}
