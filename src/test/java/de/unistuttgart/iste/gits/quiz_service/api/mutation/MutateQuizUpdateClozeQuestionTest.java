package de.unistuttgart.iste.gits.quiz_service.api.mutation;

import de.unistuttgart.iste.meitrex.common.testutil.GraphQlApiTest;
import de.unistuttgart.iste.meitrex.common.testutil.InjectCurrentUserHeader;
import de.unistuttgart.iste.meitrex.common.testutil.TablesToDelete;
import de.unistuttgart.iste.meitrex.common.user_handling.LoggedInUser;
import de.unistuttgart.iste.meitrex.generated.dto.*;
import de.unistuttgart.iste.gits.quiz_service.TestData;
import de.unistuttgart.iste.gits.quiz_service.api.QuizFragments;
import de.unistuttgart.iste.gits.quiz_service.persistence.entity.ClozeQuestionEntity;
import de.unistuttgart.iste.gits.quiz_service.persistence.entity.QuizEntity;
import de.unistuttgart.iste.gits.quiz_service.persistence.repository.QuizRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.annotation.Commit;

import java.util.List;
import java.util.UUID;

import static de.unistuttgart.iste.meitrex.common.testutil.TestUsers.userWithMembershipInCourseWithId;
import static de.unistuttgart.iste.gits.quiz_service.TestData.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

@GraphQlApiTest
@TablesToDelete({"cloze_question_additional_wrong_answers", "cloze_question_cloze_elements", "cloze_question", "quiz_question_pool", "question", "quiz"})
class MutateQuizUpdateClozeQuestionTest {

    @Autowired
    private QuizRepository quizRepository;
    private final UUID courseId = UUID.randomUUID();

    @InjectCurrentUserHeader
    private final LoggedInUser loggedInUser = userWithMembershipInCourseWithId(courseId, LoggedInUser.UserRoleInCourse.ADMINISTRATOR);

    @Test
    @Transactional
    @Commit
    void testUpdateClozeQuestion(final GraphQlTester graphQlTester) {
        QuizEntity quizEntity = TestData.exampleQuizBuilder(courseId)
                .questionPool(List.of(
                        createClozeQuestion(1,
                                clozeText("This is an example text with a "),
                                clozeBlank("blank"), clozeText("."))))
                .build();
        quizEntity = quizRepository.save(quizEntity);

        final UpdateClozeQuestionInput input = UpdateClozeQuestionInput.builder()
                .setItemId(quizEntity.getQuestionPool().get(0).getItemId())
                .setClozeElements(List.of(
                        ClozeElementInput.builder()
                                .setType(ClozeElementType.TEXT)
                                .setText("This is an example text with a blank.")
                                .build(),
                        ClozeElementInput.builder()
                                .setType(ClozeElementType.BLANK)
                                .setCorrectAnswer("blank")
                                .setFeedback("new feedback")
                                .build()))
                .setShowBlanksList(false)
                .setHint("new hint")
                .setAdditionalWrongAnswers(List.of("new wrong answer"))
                .build();

        final String query = QuizFragments.FRAGMENT_DEFINITION + """
                mutation($id: UUID!, $input: UpdateClozeQuestionInput!) {
                    mutateQuiz(assessmentId: $id) {
                        updateClozeQuestion(input: $input) {
                            ...QuizAllFields
                        }
                    }
                }
                """;

        graphQlTester.document(query)
                .variable("id", quizEntity.getAssessmentId())
                .variable("input", input)
                .execute()
                .path("mutateQuiz.updateClozeQuestion.questionPool[0].number")
                .entity(Integer.class)
                .isEqualTo(1)

                .path("mutateQuiz.updateClozeQuestion.questionPool[0].clozeElements[0]")
                .entity(ClozeTextElement.class)
                .isEqualTo(ClozeTextElement.builder()
                        .setText("This is an example text with a blank.")
                        .build())

                .path("mutateQuiz.updateClozeQuestion.questionPool[0].clozeElements[1]")
                .entity(ClozeBlankElement.class)
                .isEqualTo(ClozeBlankElement.builder()
                        .setCorrectAnswer("blank")
                        .setFeedback("new feedback")
                        .build())

                .path("mutateQuiz.updateClozeQuestion.questionPool[0].showBlanksList")
                .entity(Boolean.class)
                .isEqualTo(false)

                .path("mutateQuiz.updateClozeQuestion.questionPool[0].hint")
                .entity(String.class)
                .isEqualTo("new hint")

                .path("mutateQuiz.updateClozeQuestion.questionPool[0].additionalWrongAnswers")
                .entityList(String.class)
                .containsExactly("new wrong answer");

        final QuizEntity updatedQuiz = quizRepository.findById(quizEntity.getAssessmentId()).orElseThrow();
        assertThat(updatedQuiz.getQuestionPool(), hasSize(1));
        final ClozeQuestionEntity updatedQuestion = (ClozeQuestionEntity) updatedQuiz.getQuestionPool().get(0);
        assertThat(updatedQuestion.getHint(), is("new hint"));
        assertThat(updatedQuestion.isShowBlanksList(), is(false));
        assertThat(updatedQuestion.getAdditionalWrongAnswers(), hasSize(1));
        assertThat(updatedQuestion.getAdditionalWrongAnswers().get(0), is("new wrong answer"));
        assertThat(updatedQuestion.getClozeElements(), hasSize(2));
        assertThat(updatedQuestion.getClozeElements().get(0).getType(), is(ClozeElementType.TEXT));
        assertThat(updatedQuestion.getClozeElements().get(0).getText(), is("This is an example text with a blank."));
        assertThat(updatedQuestion.getClozeElements().get(1).getType(), is(ClozeElementType.BLANK));
        assertThat(updatedQuestion.getClozeElements().get(1).getCorrectAnswer(), is("blank"));
        assertThat(updatedQuestion.getClozeElements().get(1).getFeedback(), is("new feedback"));

    }
}
