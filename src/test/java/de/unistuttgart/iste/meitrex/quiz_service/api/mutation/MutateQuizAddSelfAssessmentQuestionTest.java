package de.unistuttgart.iste.meitrex.quiz_service.api.mutation;

import de.unistuttgart.iste.gits.quiz_service.api.QuizFragments;
import de.unistuttgart.iste.gits.quiz_service.persistence.entity.QuestionEntity;
import de.unistuttgart.iste.gits.quiz_service.persistence.entity.QuizEntity;
import de.unistuttgart.iste.gits.quiz_service.persistence.entity.SelfAssessmentQuestionEntity;
import de.unistuttgart.iste.meitrex.common.testutil.*;
import de.unistuttgart.iste.meitrex.common.user_handling.LoggedInUser;
import de.unistuttgart.iste.meitrex.generated.dto.CreateSelfAssessmentQuestionInput;
import de.unistuttgart.iste.gits.quiz_service.TestData;
import de.unistuttgart.iste.gits.quiz_service.persistence.repository.QuizRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.annotation.Commit;

import java.util.List;
import java.util.UUID;

import static de.unistuttgart.iste.meitrex.common.testutil.TestUsers.userWithMembershipInCourseWithId;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

@GraphQlApiTest
class MutateQuizAddSelfAssessmentQuestionTest {

    @Autowired
    private QuizRepository quizRepository;
    private final UUID courseId = UUID.randomUUID();

    @InjectCurrentUserHeader
    private final LoggedInUser loggedInUser = userWithMembershipInCourseWithId(courseId, LoggedInUser.UserRoleInCourse.ADMINISTRATOR);

    private static final String ADD_SELF_ASSESSMENT_QUESTION_MUTATION = QuizFragments.FRAGMENT_DEFINITION + """
            mutation($id: UUID!, $input: CreateSelfAssessmentQuestionInput!) {
                mutateQuiz(assessmentId: $id) {
                    _internal_noauth_addSelfAssessmentQuestion(input: $input) {
                        ...QuizAllFields
                    }
                }
            }
            """;

    /**
     * Given a quiz
     * When the "addSelfAssessmentQuestion" mutation is called with a new question
     * Then the new question is added to the quiz
     */
    @Test
    @Transactional
    @Commit
    void testAddSelfAssessmentQuestion(final GraphQlTester graphQlTester) {
        QuizEntity quizEntity = TestData.exampleQuizBuilder(courseId)
                .questionPool(List.of())
                .build();
        quizEntity = quizRepository.save(quizEntity);
        UUID itemId = UUID.randomUUID();
        final CreateSelfAssessmentQuestionInput input = CreateSelfAssessmentQuestionInput.builder()
                .setItemId(itemId)
                .setHint("hint")
                .setText("question")
                .setSolutionSuggestion("solution suggestion")
                .build();

        graphQlTester.document(ADD_SELF_ASSESSMENT_QUESTION_MUTATION)
                .variable("input", input)
                .variable("id", quizEntity.getAssessmentId())
                .execute()
                .path("mutateQuiz._internal_noauth_addSelfAssessmentQuestion.questionPool[0].number")
                .entity(Integer.class).isEqualTo(1)

                .path("mutateQuiz._internal_noauth_addSelfAssessmentQuestion.questionPool[0].itemId")
                .entity(UUID.class).isEqualTo(itemId)

                .path("mutateQuiz._internal_noauth_addSelfAssessmentQuestion.questionPool[0].hint")
                .entity(String.class).isEqualTo("hint")

                .path("mutateQuiz._internal_noauth_addSelfAssessmentQuestion.questionPool[0].text")
                .entity(String.class).isEqualTo("question")

                .path("mutateQuiz._internal_noauth_addSelfAssessmentQuestion.questionPool[0].solutionSuggestion")
                .entity(String.class).isEqualTo("solution suggestion");

        final QuestionEntity questionEntity = quizRepository.findById(quizEntity.getAssessmentId())
                .orElseThrow()
                .getQuestionPool()
                .get(0);

        assertThat(questionEntity, instanceOf(SelfAssessmentQuestionEntity.class));
        final SelfAssessmentQuestionEntity selfAssessmentQuestionEntity = (SelfAssessmentQuestionEntity) questionEntity;

        assertThat(selfAssessmentQuestionEntity.getItemId(), is(itemId));
        assertThat(selfAssessmentQuestionEntity.getHint(), is("hint"));
        assertThat(selfAssessmentQuestionEntity.getText(), is("question"));
        assertThat(selfAssessmentQuestionEntity.getSolutionSuggestion(), is("solution suggestion"));
    }
}
