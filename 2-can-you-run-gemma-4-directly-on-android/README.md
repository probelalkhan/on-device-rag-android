# Can You Run Gemma 4 Directly on Android

This is the resource for Episode 2 of the On Device RAG on Android course.

In this episode we build a small Android app that runs Gemma 4 directly on the phone using LiteRT LM.

## Start here

Use the `starter-project` directory if you want to follow along with the video.

It contains a basic Android project and the spec we use in the episode.

The spec is here.

`starter-project/specs/spec.md`

Open `starter-project` in Antigravity and ask it to read the spec and inspect the existing Android project.

Start by asking for an implementation plan only. Do not let it change the code yet.

Review the plan first. Once it looks reasonable, ask Antigravity to implement the spec.

After the implementation is complete, open the project in Android Studio, build it, run it on a supported Android device, and inspect the generated code.

Your generated code may not be identical to the completed project. That is fine. The important part is that it follows the same requirements and engineering decisions.

## Hugging Face token

The Gemma model is downloaded from Hugging Face.

Add your Hugging Face token to `local.properties` inside the starter project.

`HF_TOKEN=your_token_here`

Do not commit this token.

You may also need to accept the model terms on Hugging Face before the download works.

## Finished project

If your generated project does not work, or you want to compare your implementation with the version used in the video, check the `ondevice-llm-android` directory.

That is the completed Episode 2 project.

Try building from the spec first. The useful part of this exercise is seeing how the requirements turn into a working Android implementation, then checking the generated code instead of copying the final code line by line.
