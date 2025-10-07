package de.unistuttgart.iste.meitrex.quiz_service.api.mutation;

import de.unistuttgart.iste.meitrex.common.testutil.GraphQlApiTest;
import de.unistuttgart.iste.meitrex.common.testutil.InjectCurrentUserHeader;
import de.unistuttgart.iste.meitrex.common.testutil.TablesToDelete;
import de.unistuttgart.iste.meitrex.common.user_handling.LoggedInUser;
import de.unistuttgart.iste.meitrex.generated.dto.UpdateSelfAssessmentQuestionInput;
import de.unistuttgart.iste.meitrex.quiz_service.TestData;
import de.unistuttgart.iste.meitrex.quiz_service.api.QuizFragments;
import de.unistuttgart.iste.meitrex.quiz_service.persistence.entity.QuizEntity;
import de.unistuttgart.iste.meitrex.quiz_service.persistence.entity.SelfAssessmentQuestionEntity;
import de.unistuttgart.iste.meitrex.quiz_service.persistence.repository.QuizRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.annotation.Commit;
import org.springframework.test.context.ContextConfiguration;
import de.unistuttgart.iste.meitrex.common.testutil.MockTestPublisherConfiguration;

import java.util.List;
import java.util.UUID;

import static de.unistuttgart.iste.meitrex.common.testutil.TestUsers.userWithMembershipInCourseWithId;
import static de.unistuttgart.iste.meitrex.quiz_service.TestData.createSelfAssessmentQuestion;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

@GraphQlApiTest
@ContextConfiguration(classes = MockTestPublisherConfiguration.class)
class MutateQuizUpdateSelfAssessmentQuestionTest {

    @Autowired
    private QuizRepository quizRepository;
    private final UUID courseId = UUID.randomUUID();

    @InjectCurrentUserHeader
    private final LoggedInUser loggedInUser = userWithMembershipInCourseWithId(courseId, LoggedInUser.UserRoleInCourse.ADMINISTRATOR);

    @Test
    @Transactional
    @Commit
    void testUpdateSelfAssessmentQuestion(final GraphQlTester graphQlTester) {
        QuizEntity quizEntity = TestData.exampleQuizBuilder(courseId)
                .questionPool(List.of(
                        createSelfAssessmentQuestion(1, "question", "answer")))
                .build();
        quizEntity = quizRepository.save(quizEntity);

        final UpdateSelfAssessmentQuestionInput input = UpdateSelfAssessmentQuestionInput.builder()
                .setItemId(quizEntity.getQuestionPool().get(0).getItemId())
                .setHint("new hint")
                .setText("new question")
                .setSolutionSuggestion("new solution suggestion")
                .build();

        final String query = QuizFragments.FRAGMENT_DEFINITION + """
                mutation($id: UUID!, $input: UpdateSelfAssessmentQuestionInput!) {
                    mutateQuiz(assessmentId: $id) {
                        _internal_noauth_updateSelfAssessmentQuestion(input: $input) {
                            ...QuizAllFields
                        }
                    }
                }
                """;

        graphQlTester.document(query)
                .variable("id", quizEntity.getAssessmentId())
                .variable("input", input)
                .execute()
                .path("mutateQuiz._internal_noauth_updateSelfAssessmentQuestion.questionPool[0].number").entity(Integer.class).isEqualTo(1)
                .path("mutateQuiz._internal_noauth_updateSelfAssessmentQuestion.questionPool[0].text").entity(String.class).isEqualTo("new question")
                .path("mutateQuiz._internal_noauth_updateSelfAssessmentQuestion.questionPool[0].hint").entity(String.class).isEqualTo("new hint")
                .path("mutateQuiz._internal_noauth_updateSelfAssessmentQuestion.questionPool[0].solutionSuggestion").entity(String.class).isEqualTo("new solution suggestion");

        final QuizEntity updatedQuiz = quizRepository.findById(quizEntity.getAssessmentId()).orElseThrow();
        assertThat(updatedQuiz.getQuestionPool(), hasSize(1));
        final SelfAssessmentQuestionEntity updatedQuestion = (SelfAssessmentQuestionEntity) updatedQuiz.getQuestionPool().get(0);
        assertThat(updatedQuestion.getText(), is("new question"));
        assertThat(updatedQuestion.getHint(), is("new hint"));
        assertThat(updatedQuestion.getSolutionSuggestion(), is("new solution suggestion"));
    }
}
