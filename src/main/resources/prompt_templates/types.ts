// used to generate the json schema with gen AI, i.e. Mistral or Github Copilot

export interface Quiz {
    title: string
    questions: Question[]
}

export interface Question {
    question: string
    type: "multiple_choice" | "free_text" | "numeric" | "exact_answer"
    type_options : {
        options?: {
            text: string,
            is_correct: boolean,
        },
        answer?: string | number,
        max_difference: number,
        case_sensitive: boolean,
    }
}

export interface Gen{
    quiz: Quiz;
}