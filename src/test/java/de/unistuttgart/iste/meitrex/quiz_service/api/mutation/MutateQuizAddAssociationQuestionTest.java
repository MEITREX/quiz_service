package de.unistuttgart.iste.meitrex.quiz_service.api.mutation;

import de.unistuttgart.iste.meitrex.common.testutil.GraphQlApiTest;
import de.unistuttgart.iste.meitrex.common.testutil.InjectCurrentUserHeader;
import de.unistuttgart.iste.meitrex.common.testutil.TablesToDelete;
import de.unistuttgart.iste.meitrex.common.user_handling.LoggedInUser;
import de.unistuttgart.iste.meitrex.generated.dto.AssociationInput;
import de.unistuttgart.iste.meitrex.generated.dto.CreateAssociationQuestionInput;
import de.unistuttgart.iste.meitrex.generated.dto.SingleAssociation;
import de.unistuttgart.iste.meitrex.quiz_service.TestData;
import de.unistuttgart.iste.meitrex.quiz_service.api.QuizFragments;
import de.unistuttgart.iste.meitrex.quiz_service.persistence.entity.AssociationEmbeddable;
import de.unistuttgart.iste.meitrex.quiz_service.persistence.entity.AssociationQuestionEntity;
import de.unistuttgart.iste.meitrex.quiz_service.persistence.entity.QuestionEntity;
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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;


@GraphQlApiTest
@TablesToDelete({"association_question_correct_associations", "association_question", "quiz_question_pool", "question", "quiz"})
class MutateQuizAddAssociationQuestionTest {

    @Autowired
    private QuizRepository quizRepository;
    private final UUID courseId = UUID.randomUUID();

    @InjectCurrentUserHeader
    private final LoggedInUser loggedInUser = userWithMembershipInCourseWithId(courseId, LoggedInUser.UserRoleInCourse.ADMINISTRATOR);


    private static final String ADD_ASSOCIATION_QUESTION_MUTATION = QuizFragments.FRAGMENT_DEFINITION + """
            mutation($id: UUID!, $input: CreateAssociationQuestionInput!) {
                mutateQuiz(assessmentId: $id) {
                    addAssociationQuestion(input: $input) {
                        ...QuizAllFields
                    }
                }
            }
            """;

    /**
     * Given a quiz
     * When the "addAssociationQuestion" mutation is called with a new question
     * Then the new question is added to the quiz
     */
    @Test
    @Transactional
    @Commit
    void testAddAssociationQuestion(final GraphQlTester graphQlTester) {
        QuizEntity quizEntity = TestData.exampleQuizBuilder(courseId)
                .questionPool(List.of())
                .build();
        quizEntity = quizRepository.save(quizEntity);

        final CreateAssociationQuestionInput input = CreateAssociationQuestionInput.builder()
                .setHint("hint")
                .setText("question")
                .setCorrectAssociations(List.of(
                        new AssociationInput("a", "b", "feedback1"),
                        new AssociationInput("c", "d", "feedback2")))
                .build();


        graphQlTester.document(ADD_ASSOCIATION_QUESTION_MUTATION)
                .variable("input", input)
                .variable("id", quizEntity.getAssessmentId())
                .execute()
                .path("mutateQuiz.addAssociationQuestion.questionPool[0].number")
                .entity(Integer.class)
                .isEqualTo(1)

                .path("mutateQuiz.addAssociationQuestion.questionPool[0].text")
                .entity(String.class)
                .isEqualTo("question")

                .path("mutateQuiz.addAssociationQuestion.questionPool[0].hint")
                .entity(String.class)
                .isEqualTo("hint")

                .path("mutateQuiz.addAssociationQuestion.questionPool[0].correctAssociations")
                .entityList(SingleAssociation.class)
                .contains(
                        new SingleAssociation("a", "b", "feedback1"),
                        new SingleAssociation("c", "d", "feedback2"))

                .path("mutateQuiz.addAssociationQuestion.questionPool[0].leftSide")
                .entityList(String.class)
                .contains("a", "c")

                .path("mutateQuiz.addAssociationQuestion.questionPool[0].rightSide")
                .entityList(String.class)
                .contains("b", "d");

        final QuestionEntity questionEntity = quizRepository.findById(quizEntity.getAssessmentId())
                .orElseThrow()
                .getQuestionPool()
                .get(0);

        assertThat(questionEntity, instanceOf(AssociationQuestionEntity.class));
        final AssociationQuestionEntity associationQuestionEntity = (AssociationQuestionEntity) questionEntity;

        assertThat(associationQuestionEntity.getHint(), is("hint"));
        assertThat(associationQuestionEntity.getText(), is("question"));
        assertThat(associationQuestionEntity.getCorrectAssociations(), hasSize(2));
        assertThat(associationQuestionEntity.getCorrectAssociations(), containsInAnyOrder(
                new AssociationEmbeddable("a", "b", "feedback1"),
                new AssociationEmbeddable("c", "d", "feedback2")));

    }

    /**
     * Given a quiz and an association question where the associations are not unique
     * When the "addAssociationQuestion" mutation is called with a new question
     * Then the new question is added to the quiz
     */
    @Test
    @Transactional
    void testAddAssociationQuestionNotUniqueAnswer(final GraphQlTester graphQlTester) {
        QuizEntity quizEntity = TestData.exampleQuizBuilder(courseId)
                .questionPool(List.of())
                .build();
        quizEntity = quizRepository.save(quizEntity);

        final CreateAssociationQuestionInput input = CreateAssociationQuestionInput.builder()
                .setNumber(2)
                .setHint("hint")
                .setText("question")
                .setCorrectAssociations(List.of(
                        new AssociationInput("a", "b", "feedback1"),
                        new AssociationInput("c", "b", "feedback2")))
                .build();

        graphQlTester.document(ADD_ASSOCIATION_QUESTION_MUTATION)
                .variable("input", input)
                .variable("id", quizEntity.getAssessmentId())
                .execute()
                .errors()
                .satisfy(errors -> {
                    assertThat(errors, hasSize(1));
                    assertThat(errors.get(0).getMessage(), containsString("Each side of the associations must only contain unique values"));
                });
    }
}
