package de.unistuttgart.iste.meitrex.quiz_service.service;

import de.unistuttgart.iste.meitrex.quiz_service.service.model.OllamaRequest;
import de.unistuttgart.iste.meitrex.quiz_service.service.model.OllamaResponse;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.nio.file.Files;

import static org.mockito.Mockito.*;

public class OllamaServiceTest {



    // mock HttpClient
    private final HttpClient httpClient = Mockito.mock(HttpClient.class);
    private final OllamaService ollamaService = new OllamaService(httpClient);

    @Test
    public void testOllamaService() throws IOException, InterruptedException {
        String testPrompt = "Your are a helpful assistant that is assisting in generating a quiz for students. \\n\\nThe quiz has the following description:\\nA general knowledge quiz about computers and technology to get started with the basics of computer science. \\n\\nYou will get provided the following ressources to produce a quiz of:\\n\\n1. The first electronic general-purpose computer was ENIAC, completed in 1945.  \\n---------\\n2. The term \\\"computer\\\" originally referred to a human who performed numerical calculations.  \\n---------\\n3. A byte consists of 8 bits and is the standard unit for measuring data.  \\n---------\\n4. The Central Processing Unit (CPU) is considered the brain of the computer.  \\n---------\\n5. RAM (Random Access Memory) is volatile memory that temporarily stores data while a computer is on.  \\n---------\\n6. The first computer mouse was made of wood and invented in the 1960s by Douglas Engelbart.  \\n---------\\n7. Computers operate using binary code, which consists of only 0s and 1s.  \\n---------\\n8. Moore’s Law states that the number of transistors on a chip doubles about every two years.  \\n---------\\n9. An operating system (OS) is software that manages hardware and software resources on a computer.  \\n---------\\n10. The first commercially available computer was the UNIVAC I in 1951.  \\n---------\\n11. A solid-state drive (SSD) is faster and more durable than a traditional hard disk drive (HDD).  \\n---------\\n12. The Internet was originally developed as a project called ARPANET in the late 1960s.  \\n---------\\n13. A computer virus is a type of malicious software designed to spread and cause damage.  \\n---------\\n14. Quantum computers use qubits and can perform certain calculations much faster than classical computers.  \\n---------\\n15. The Turing machine, proposed by Alan Turing, is a theoretical model of computation.  \\n---------\\n16. A GPU (Graphics Processing Unit) accelerates rendering of images and video and is also used in AI tasks.  \\n---------\\n17. Computers use input devices like keyboards and mice, and output devices like monitors and printers.  \\n---------\\n18. Linux is an open-source operating system widely used for servers and embedded systems.  \\n---------\\n19. ASCII stands for American Standard Code for Information Interchange and encodes text in computers.  \\n---------\\n20. The cloud refers to remote servers accessed over the Internet to store and process data.  \\n---------\\n21. Firewalls are security systems that monitor and control incoming and outgoing network traffic.  \\n---------\\n22. The BIOS (Basic Input/Output System) is firmware that initializes hardware during the boot process.  \\n---------\\n23. A compiler translates code written in high-level programming languages into machine code.  \\n---------\\n24. Virtual machines allow multiple operating systems to run on a single physical machine.  \\n---------\\n25. CAPTCHA stands for \\\"Completely Automated Public Turing test to tell Computers and Humans Apart.\\\"  \\n---------\\n\\n\\nThe following question types are supported:\\n- multiple_choice\\n- free_text\\n\\nThe question types have the following type_options:\\n- multiple_choice:\\n    - options: a list of options for the question, each option has a text and a is_correct boolean\\n- free_text:\\n    - answer: the answer to the question, this is a string\\n\\n\\nThe following limitations apply:\\n\\nonly one corret answer per question in multiple choice questions\\n\\nThe output shall be json formatted with \\n{\\n    \\\"quiz\\\": {\\n        \\\"title\\\": \\\"Quiz Title\\\",\\n        \\\"questions\\\": [\\n            {\\n                \\\"question\\\": \\\"question text\\\",\\n                \\\"type\\\": \\\"multiple_choice\\\",\\n                \\\"type_options\\\": {\\n                    \\\"options\\\": [\\n                        {\\n                            \\\"text\\\": \\\"option 1\\\",\\n                            \\\"is_correct\\\": true\\n                        },\\n                        {\\n                            \\\"text\\\": \\\"option 2\\\",\\n                            \\\"is_correct\\\": false\\n                        },\\n                    ]\\n                },\\n            },\\n            ...\\n        ]\\n    }\\n}\\n\\n Create 4 questions and give your give your answer in JSON without an introduction: {";
        final OllamaRequest request = new OllamaRequest(
                "mistral-nemo",
                testPrompt
        );

        // load json from file in resources
        final String jsonPath =  this.getClass().getClassLoader().getResource("ollama_sample.json").getPath();
        final String json = new String(Files.readAllBytes(java.nio.file.Paths.get(jsonPath)));

        // mock the http request with the response usally return by ollama
        HttpResponse<Object> mockResponse = mock(HttpResponse.class);
        when(mockResponse.body()).thenReturn(json);
        when(mockResponse.statusCode()).thenReturn(200);

        when(httpClient.send(any(),any())).thenReturn(mockResponse);

        final OllamaResponse res = ollamaService.queryLLM(request);
        assert res.getModel().equals("mistral-nemo");
        assert res.getEvalCount() == 321;
        assert res.getPromptEvalCount() == 890;


    }
}
