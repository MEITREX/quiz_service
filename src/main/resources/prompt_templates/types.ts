// used to generate the json schema with gen AI, i.e. Mistral or Github Copilot or use typescript-json-schema instead

export interface Quiz {
    title: string
    questions: Questions
}

export interface Questions{
    multiple_choice: MultipleChoiceQuestion[];
    free_text: FreeTextQuestion[];
    numeric: NumericQuestion[];
    exact_answer: ExactAnswerQuestion[];
}

export interface MultipleChoiceQuestion {
    question: string
    options: Array<{
        text: string;
        is_correct: boolean;
    }>;
}
export interface FreeTextQuestion {
    question: string;
    answer: string;
}
export interface NumericQuestion {
    question: string;
    answer: number;
    max_difference: number;
}
export interface ExactAnswerQuestion {
    question: string;
    answer: string;
    case_sensitive: boolean;
}


export interface Gen{
    quiz: Quiz;
}

