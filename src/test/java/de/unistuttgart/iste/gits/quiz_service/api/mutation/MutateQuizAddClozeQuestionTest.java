package de.unistuttgart.iste.gits.quiz_service.api.mutation;

import de.unistuttgart.iste.gits.quiz_service.persistence.entity.ClozeQuestionEntity;
import de.unistuttgart.iste.gits.quiz_service.persistence.entity.QuestionEntity;
import de.unistuttgart.iste.gits.quiz_service.persistence.entity.QuizEntity;
import de.unistuttgart.iste.meitrex.common.testutil.GraphQlApiTest;
import de.unistuttgart.iste.meitrex.common.testutil.InjectCurrentUserHeader;
import de.unistuttgart.iste.meitrex.common.testutil.TablesToDelete;
import de.unistuttgart.iste.meitrex.common.user_handling.LoggedInUser;
import de.unistuttgart.iste.meitrex.generated.dto.*;
import de.unistuttgart.iste.gits.quiz_service.TestData;
import de.unistuttgart.iste.gits.quiz_service.api.QuizFragments;
import de.unistuttgart.iste.gits.quiz_service.persistence.repository.QuizRepository;
import graphql.ErrorType;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.annotation.Commit;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static de.unistuttgart.iste.meitrex.common.testutil.TestUsers.userWithMembershipInCourseWithId;


@GraphQlApiTest
@TablesToDelete({"cloze_question_additional_wrong_answers", "cloze_question_cloze_elements", "cloze_question", "quiz_question_pool", "question", "quiz"})
class MutateQuizAddClozeQuestionTest {

    private static final String ADD_CLOZE_QUESTION_MUTATION = QuizFragments.FRAGMENT_DEFINITION + """
            mutation($id: UUID!, $input: CreateClozeQuestionInput!) {
                mutateQuiz(assessmentId: $id) {
                    _internal_noauth_addClozeQuestion(input: $input) {
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
     * When the "addClozeQuestion" mutation is called with a new question
     * Then the new question is added to the quiz
     */
    @Test
    @Transactional
    @Commit
    void testAddClozeQuestion(final GraphQlTester graphQlTester) {
        QuizEntity quizEntity = TestData.exampleQuizBuilder(courseId)
                .questionPool(List.of())
                .build();
        quizEntity = quizRepository.save(quizEntity);
        UUID itemId=UUID.randomUUID();
        final CreateClozeQuestionInput input = CreateClozeQuestionInput.builder()
                .setItemId(itemId)
                .setHint("hint")
                .setAdditionalWrongAnswers(List.of("wrong1", "wrong2"))
                .setShowBlanksList(false)
                .setClozeElements(List.of(
                        ClozeElementInput.builder()
                                .setType(ClozeElementType.TEXT)
                                .setText("what is the capital of France?")
                                .build(),
                        ClozeElementInput.builder()
                                .setType(ClozeElementType.BLANK)
                                .setCorrectAnswer("Paris")
                                .setFeedback("feedback")
                                .build()
                ))
                .build();


        graphQlTester.document(ADD_CLOZE_QUESTION_MUTATION)
                .variable("input", input)
                .variable("id", quizEntity.getAssessmentId())
                .execute()
                .path("mutateQuiz._internal_noauth_addClozeQuestion.questionPool[0].number")
                .entity(Integer.class)
                .isEqualTo(1)

                .path("mutateQuiz._internal_noauth_addClozeQuestion.questionPool[0].itemId")
                .entity(UUID.class)
                .isEqualTo(itemId)

                .path("mutateQuiz._internal_noauth_addClozeQuestion.questionPool[0].showBlanksList")
                .entity(Boolean.class)
                .isEqualTo(false)

                .path("mutateQuiz._internal_noauth_addClozeQuestion.questionPool[0].additionalWrongAnswers")
                .entityList(String.class)
                .isEqualTo(List.of("wrong1", "wrong2"))

                .path("mutateQuiz._internal_noauth_addClozeQuestion.questionPool[0].clozeElements[0].text")
                .entity(String.class)
                .isEqualTo("what is the capital of France?")

                .path("mutateQuiz._internal_noauth_addClozeQuestion.questionPool[0].clozeElements[1].correctAnswer")
                .entity(String.class)
                .isEqualTo("Paris")

                .path("mutateQuiz._internal_noauth_addClozeQuestion.questionPool[0].clozeElements[1].feedback")
                .entity(String.class)
                .isEqualTo("feedback");

        final QuestionEntity questionEntity = quizRepository.findById(quizEntity.getAssessmentId())
                .orElseThrow()
                .getQuestionPool()
                .get(0);

        assertThat(questionEntity, instanceOf(ClozeQuestionEntity.class));
        final ClozeQuestionEntity clozeQuestionEntity = (ClozeQuestionEntity) questionEntity;
        assertThat(clozeQuestionEntity.getAdditionalWrongAnswers(), contains("wrong1", "wrong2"));
        assertThat(clozeQuestionEntity.getClozeElements(), hasSize(2));
        assertThat(clozeQuestionEntity.getClozeElements().get(0).getType(), is(ClozeElementType.TEXT));
        assertThat(clozeQuestionEntity.getClozeElements().get(0).getText(), is("what is the capital of France?"));
        assertThat(clozeQuestionEntity.getClozeElements().get(0).getPosition(), is(1));
        assertThat(clozeQuestionEntity.getClozeElements().get(1).getType(), is(ClozeElementType.BLANK));
        assertThat(clozeQuestionEntity.getClozeElements().get(1).getCorrectAnswer(), is("Paris"));
        assertThat(clozeQuestionEntity.getClozeElements().get(1).getFeedback(), is("feedback"));
        assertThat(clozeQuestionEntity.getClozeElements().get(1).getPosition(), is(2));

        assertThat(clozeQuestionEntity.getHint(), is("hint"));
    }

    /**
     * Given a quiz
     * When the "addClozeQuestion" mutation is called with no blank
     * Then an error is returned
     */
    @Test
    void testAddClozeWithoutBlank(final GraphQlTester graphQlTester) {
        QuizEntity quizEntity = TestData.exampleQuizBuilder(courseId)
                .questionPool(List.of())
                .build();
        quizEntity = quizRepository.save(quizEntity);
        UUID itemId=UUID.randomUUID();
        final CreateClozeQuestionInput input = CreateClozeQuestionInput.builder()
                .setItemId(itemId)
                .setNumber(1)
                .setShowBlanksList(true)
                .setClozeElements(List.of(
                        ClozeElementInput.builder()
                                .setType(ClozeElementType.TEXT)
                                .setText("Some text")
                                .build(),
                        ClozeElementInput.builder()
                                .setType(ClozeElementType.TEXT)
                                .setText("text")
                                .build()))
                .build();

        graphQlTester.document(ADD_CLOZE_QUESTION_MUTATION)
                .variable("input", input)
                .variable("id", quizEntity.getAssessmentId())
                .execute()
                .errors()
                .satisfy(errors -> {
                    assertThat(errors, hasSize(1));
                    assertThat(errors.get(0).getMessage(),
                            containsString("most contain at least one blank"));
                    assertThat(errors.get(0).getErrorType(), is(ErrorType.ValidationError));
                });
    }

    @Test
    void addClozeTextElementWithFeedback(final GraphQlTester graphQlTester) {
        QuizEntity quizEntity = TestData.exampleQuizBuilder(courseId)
                .questionPool(List.of())
                .build();
        quizEntity = quizRepository.save(quizEntity);
        UUID itemId=UUID.randomUUID();
        final CreateClozeQuestionInput input = CreateClozeQuestionInput.builder()
                .setItemId(itemId)
                .setNumber(1)
                .setShowBlanksList(true)
                .setClozeElements(List.of(
                        ClozeElementInput.builder()
                                .setType(ClozeElementType.TEXT)
                                .setText("Some text")
                                .setFeedback("feedback")
                                .build(),
                        ClozeElementInput.builder()
                                .setType(ClozeElementType.BLANK)
                                .setCorrectAnswer("correct")
                                .build()))
                .build();

        graphQlTester.document(ADD_CLOZE_QUESTION_MUTATION)
                .variable("input", input)
                .variable("id", quizEntity.getAssessmentId())
                .execute()
                .errors()
                .satisfy(errors -> {
                    assertThat(errors, hasSize(1));
                    assertThat(errors.get(0).getMessage(),
                            containsString("text elements cannot have feedback"));
                    assertThat(errors.get(0).getErrorType(), is(ErrorType.ValidationError));
                });
    }

    @Test
    void addClozeElementWithCorrectAnswer(final GraphQlTester graphQlTester) {
        QuizEntity quizEntity = TestData.exampleQuizBuilder(courseId)
                .questionPool(List.of())
                .build();
        quizEntity = quizRepository.save(quizEntity);
        UUID itemId=UUID.randomUUID();
        final CreateClozeQuestionInput input = CreateClozeQuestionInput.builder()
                .setItemId(itemId)
                .setNumber(1)
                .setShowBlanksList(true)
                .setClozeElements(List.of(
                        ClozeElementInput.builder()
                                .setType(ClozeElementType.TEXT)
                                .setCorrectAnswer("correct")
                                .build(),
                        ClozeElementInput.builder()
                                .setType(ClozeElementType.BLANK)
                                .setCorrectAnswer("correct")
                                .build()))
                .build();

        graphQlTester.document(ADD_CLOZE_QUESTION_MUTATION)
                .variable("input", input)
                .variable("id", quizEntity.getAssessmentId())
                .execute()
                .errors()
                .satisfy(errors -> {
                    assertThat(errors, hasSize(1));
                    assertThat(errors.get(0).getMessage(),
                            containsString("correct answer is not allowed for text elements"));
                    assertThat(errors.get(0).getErrorType(), is(ErrorType.ValidationError));
                });
    }

    @Test
    void addClozeTextElementWithoutText(final GraphQlTester graphQlTester) {
        QuizEntity quizEntity = TestData.exampleQuizBuilder(courseId)
                .questionPool(List.of())
                .build();
        quizEntity = quizRepository.save(quizEntity);
        UUID itemId=UUID.randomUUID();
        final CreateClozeQuestionInput input = CreateClozeQuestionInput.builder()
                .setItemId(itemId)
                .setNumber(1)
                .setShowBlanksList(true)
                .setClozeElements(List.of(
                        ClozeElementInput.builder()
                                .setType(ClozeElementType.TEXT)
                                .build(),
                        ClozeElementInput.builder()
                                .setType(ClozeElementType.BLANK)
                                .setCorrectAnswer("correct")
                                .build()))
                .build();

        graphQlTester.document(ADD_CLOZE_QUESTION_MUTATION)
                .variable("input", input)
                .variable("id", quizEntity.getAssessmentId())
                .execute()
                .errors()
                .satisfy(errors -> {
                    assertThat(errors, hasSize(1));
                    assertThat(errors.get(0).getMessage(),
                            containsString("text is required for cloze text elements"));
                    assertThat(errors.get(0).getErrorType(), is(ErrorType.ValidationError));
                });
    }

    @Test
    void testAddBlankWithoutCorrectAnswer(final GraphQlTester tester) {
        QuizEntity quizEntity = TestData.exampleQuizBuilder(courseId)
                .questionPool(List.of())
                .build();
        quizEntity = quizRepository.save(quizEntity);
        UUID itemId=UUID.randomUUID();
        final CreateClozeQuestionInput input = CreateClozeQuestionInput.builder()
                .setItemId(itemId)
                .setNumber(1)
                .setShowBlanksList(true)
                .setClozeElements(List.of(
                        ClozeElementInput.builder()
                                .setType(ClozeElementType.BLANK)
                                .build()))
                .build();

        tester.document(ADD_CLOZE_QUESTION_MUTATION)
                .variable("input", input)
                .variable("id", quizEntity.getAssessmentId())
                .execute()
                .errors()
                .satisfy(errors -> {
                    assertThat(errors, hasSize(1));
                    assertThat(errors.get(0).getMessage(),
                            containsString("correct answer is required for cloze blank elements"));
                    assertThat(errors.get(0).getErrorType(), is(ErrorType.ValidationError));
                });
    }

    @Test
    void testAddBlankWithText(final GraphQlTester graphQlTester) {
        QuizEntity quizEntity = TestData.exampleQuizBuilder(courseId)
                .questionPool(List.of())
                .build();
        quizEntity = quizRepository.save(quizEntity);
        UUID itemId=UUID.randomUUID();
        final CreateClozeQuestionInput input = CreateClozeQuestionInput.builder()
                .setItemId(itemId)
                .setNumber(1)
                .setShowBlanksList(true)
                .setClozeElements(List.of(
                        ClozeElementInput.builder()
                                .setType(ClozeElementType.BLANK)
                                .setText("text")
                                .setCorrectAnswer("correct")
                                .build()))
                .build();

        graphQlTester.document(ADD_CLOZE_QUESTION_MUTATION)
                .variable("input", input)
                .variable("id", quizEntity.getAssessmentId())
                .execute()
                .errors()
                .satisfy(errors -> {
                    assertThat(errors, hasSize(1));
                    assertThat(errors.get(0).getMessage(),
                            containsString("text is not allowed for cloze blank elements"));
                    assertThat(errors.get(0).getErrorType(), is(ErrorType.ValidationError));
                });
    }

}
