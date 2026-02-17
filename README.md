# ECProperty Completion | Plugin

![Static Badge](https://img.shields.io/badge/plugin%20-%20intellij?label=intellij)

## Introduction & Purpose

A IntelliJ IDEA plugin that enables developer-defined, 
hierarchical code completion. Stop searching through documentation and let the IDE suggest 
your custom property keys, descriptions, and default values as you type.

You miss some properties, when you work on your projects? 
Define them once, upload them and let the IDE suggest them for you. Hereafter you see the JSON-structure the
plugin expects from you:

```
[
    {
        "name": "some.custom.property"
    },
    {
        "name": "some.second.custom.property",
        "description": "With description"
    },
    {
        "name": "another.custom.property",
        "description": "Another description."
    }
]
```

> [NOTE]
> Description is optional, you can provide any json-file with the format about and it should work.

## How to use

1. Go to <b>Settings > Plugins > Marketplace</b>, search for "ECProperty Completion" and install it.
2. Go to <b>Settings > Other Settings > ECProperty Completion</b>.
   3. Add a new row with the <b>+</b> sign.
   4. A dialog opens, select your JSON-File with the right format.
3. Enjoy.

## For Developers (Contributing)

Clone the project, open it up in Intellij and make sure you use 21 JDK. Found any issue or want to expand the plugin? Open an issue and let's discuss!

## Execute, Tests and Experiment

Within the default project structure, there is a `.run` directory provided containing predefined *Run/Debug
configurations* that expose corresponding Gradle tasks:

| Configuration name | Description                                                                                                                                                                         |
|--------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Run Plugin         | Runs [`:runIde`][gh:intellij-platform-gradle-plugin-runIde] IntelliJ Platform Gradle Plugin task. Use the *Debug* icon for plugin debugging.                                        |
| Run Tests          | Runs [`:test`][gradle:lifecycle-tasks] Gradle task.                                                                                                                                 |
| Run Verifications  | Runs [`:verifyPlugin`][gh:intellij-platform-gradle-plugin-verifyPlugin] IntelliJ Platform Gradle Plugin task to check the plugin compatibility against the specified IntelliJ IDEs. |

> [!NOTE]
> You can find the logs from the running task in the `idea.log` tab.
