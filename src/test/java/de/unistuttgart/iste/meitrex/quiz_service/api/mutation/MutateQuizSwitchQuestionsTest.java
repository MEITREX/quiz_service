package de.unistuttgart.iste.meitrex.quiz_service.api.mutation;

import de.unistuttgart.iste.gits.quiz_service.TestData;
import de.unistuttgart.iste.gits.quiz_service.api.QuizFragments;
import de.unistuttgart.iste.gits.quiz_service.matcher.MultipleChoiceQuestionDtoToEntityMatcher;
import de.unistuttgart.iste.gits.quiz_service.persistence.entity.QuestionEntity;
import de.unistuttgart.iste.gits.quiz_service.persistence.entity.QuizEntity;
import de.unistuttgart.iste.gits.quiz_service.persistence.repository.QuizRepository;
import de.unistuttgart.iste.meitrex.common.testutil.GraphQlApiTest;
import de.unistuttgart.iste.meitrex.common.testutil.InjectCurrentUserHeader;
import de.unistuttgart.iste.meitrex.common.testutil.TablesToDelete;
import de.unistuttgart.iste.meitrex.common.user_handling.LoggedInUser;
import de.unistuttgart.iste.meitrex.generated.dto.MultipleChoiceQuestion;

import jakarta.transaction.Transactional;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.annotation.Commit;

import java.util.List;
import java.util.UUID;

import static de.unistuttgart.iste.meitrex.common.testutil.TestUsers.userWithMembershipInCourseWithId;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@GraphQlApiTest
class MutateQuizSwitchQuestionsTest {

    @Autowired
    private QuizRepository quizRepository;
    private final UUID courseId = UUID.randomUUID();

    @InjectCurrentUserHeader
    private final LoggedInUser loggedInUser = userWithMembershipInCourseWithId(courseId, LoggedInUser.UserRoleInCourse.ADMINISTRATOR);

    /**
     * Given a quiz with 3 questions
     * When the "switchQuestions" mutation is called with the first and second question number
     * Then the questions are switched
     */
    @Test
    @Transactional
    @Commit
    void testSwitchQuestion(final GraphQlTester graphQlTester) {
        // store questions in separate variable because spring apparently caches the quiz entity instance
        // so the following quiz entity instance is updated during the mutation
        final List<QuestionEntity> questionEntities = List.of(
                TestData.createMultipleChoiceQuestion(1, "what is the capital of Germany?", "Berlin", "Paris"),
                TestData.createMultipleChoiceQuestion(2, "what is the capital of France?", "Paris", "Madrid"),
                TestData.createMultipleChoiceQuestion(3, "what is the capital of Spain?", "Madrid", "Berlin"));

        QuizEntity quizEntity = TestData.exampleQuizBuilder(courseId)
                .questionPool(questionEntities)
                .build();
        quizEntity = quizRepository.save(quizEntity);

        final String query = QuizFragments.FRAGMENT_DEFINITION + """
                mutation($id: UUID!) {
                    mutateQuiz(assessmentId: $id) {
                        switchQuestions(firstNumber: 2, secondNumber: 3) {
                            ...QuizAllFields
                        }
                    }
                }
                """;
        final List<MultipleChoiceQuestion> questions = graphQlTester.document(query)
                .variable("id", quizEntity.getAssessmentId())
                .execute()
                .path("mutateQuiz.switchQuestions.questionPool")
                .entityList(MultipleChoiceQuestion.class)
                .get();

        assertThat(questions, hasSize(3));

        final QuestionEntity expectedFirstEntity = questionEntities.get(0);
        MatcherAssert.assertThat(questions.get(0), MultipleChoiceQuestionDtoToEntityMatcher.matchesEntity(expectedFirstEntity));

        final QuestionEntity expectedSecondEntity = questionEntities.get(2);
        expectedSecondEntity.setNumber(2);
        MatcherAssert.assertThat(questions.get(1), MultipleChoiceQuestionDtoToEntityMatcher.matchesEntity(expectedSecondEntity));

        final QuestionEntity expectedThirdEntity = questionEntities.get(1);
        expectedThirdEntity.setNumber(3);
        MatcherAssert.assertThat(questions.get(2), MultipleChoiceQuestionDtoToEntityMatcher.matchesEntity(expectedThirdEntity));

        final QuizEntity newQuizEntity = quizRepository.findById(quizEntity.getAssessmentId()).orElseThrow();
        assertThat(newQuizEntity.getQuestionPool(), hasSize(3));

        assertThat(newQuizEntity.getQuestionPool().get(0), is(expectedFirstEntity));
        assertThat(newQuizEntity.getQuestionPool().get(1), is(expectedSecondEntity));
        assertThat(newQuizEntity.getQuestionPool().get(2), is(expectedThirdEntity));
    }

    /**
     * Given a quiz with 2 questions
     * When the "switchQuestion" mutation is called with a non-existing question number
     * Then an error is returned
     */
    @Test
    void testSwotchQuestionNonExisting(final GraphQlTester graphQlTester) {
        QuizEntity quizEntity = TestData.exampleQuizBuilder(courseId)
                .questionPool(List.of(
                        TestData.createMultipleChoiceQuestion(1, "what is the capital of Germany?", "Berlin", "Paris"),
                        TestData.createMultipleChoiceQuestion(2, "what is the capital of France?", "Paris", "Madrid")))
                .build();
        quizEntity = quizRepository.save(quizEntity);

        final String query = """
                mutation($id: UUID!) {
                    mutateQuiz(assessmentId: $id) {
                        switchQuestions(firstNumber: 1, secondNumber: 3) {
                            questionPool {
                                number
                            }
                        }
                    }
                }
                """;
        graphQlTester.document(query)
                .variable("id", quizEntity.getAssessmentId())
                .execute()
                .errors()
                .satisfy(errors -> {
                    assertThat(errors, hasSize(1));
                    assertThat(errors.get(0).getMessage(), containsString("Question with number 3 not found"));
                });
    }
}
