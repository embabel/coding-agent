![Build](https://github.com/embabel/embabel-agent/actions/workflows/maven.yml/badge.svg)

[//]: # ([![Quality Gate Status]&#40;https://sonarcloud.io/api/project_badges/measure?project=embabel_embabel-agent&metric=alert_status&token=d275d89d09961c114b8317a4796f84faf509691c&#41;]&#40;https://sonarcloud.io/summary/new_code?id=embabel_embabel-agent&#41;)

[//]: # ([![Bugs]&#40;https://sonarcloud.io/api/project_badges/measure?project=embabel_embabel-agent&metric=bugs&#41;]&#40;https://sonarcloud.io/summary/new_code?id=embabel_embabel-agent&#41;)

![Kotlin](https://img.shields.io/badge/kotlin-%237F52FF.svg?style=for-the-badge&logo=kotlin&logoColor=white)
![Java](https://img.shields.io/badge/java-%23ED8B00.svg?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring](https://img.shields.io/badge/spring-%236DB33F.svg?style=for-the-badge&logo=spring&logoColor=white)
![Apache Tomcat](https://img.shields.io/badge/apache%20tomcat-%23F8DC75.svg?style=for-the-badge&logo=apache-tomcat&logoColor=black)
![Apache Maven](https://img.shields.io/badge/Apache%20Maven-C71A36?style=for-the-badge&logo=Apache%20Maven&logoColor=white)
![ChatGPT](https://img.shields.io/badge/chatGPT-74aa9c?style=for-the-badge&logo=openai&logoColor=white)
![Jinja](https://img.shields.io/badge/jinja-white.svg?style=for-the-badge&logo=jinja&logoColor=black)
![JSON](https://img.shields.io/badge/JSON-000?logo=json&logoColor=fff)
![GitHub Actions](https://img.shields.io/badge/github%20actions-%232671E5.svg?style=for-the-badge&logo=githubactions&logoColor=white)
![SonarQube](https://img.shields.io/badge/SonarQube-black?style=for-the-badge&logo=sonarqube&logoColor=4E9BCD)
![Docker](https://img.shields.io/badge/docker-%230db7ed.svg?style=for-the-badge&logo=docker&logoColor=white)
![IntelliJ IDEA](https://img.shields.io/badge/IntelliJIDEA-000000.svg?style=for-the-badge&logo=intellij-idea&logoColor=white)

<img align="left" src="https://github.com/embabel/embabel-agent/blob/main/embabel-agent-api/images/315px-Meister_der_Weltenchronik_001.jpg?raw=true" width="180">

&nbsp;&nbsp;&nbsp;&nbsp;

&nbsp;&nbsp;&nbsp;&nbsp;

# Embabel Coding Agent

Headless coding agent built on Embabel agent platform, for use in developing Embabel and
as an open source project in its own right.

## Aims

This project aims to provide a full-fledged coding agent that means the
Embabel team's work is accelerated by AI, yet without the use of any commercial
coding agents.

Key capabilities include:

- Provide the ability to explain and improve code
- Combine access to project code with internet access: for example, to research new APIs.

> This project is in an early stage of development.

## Futures

- Support for build systems other than Maven
- Better project selection. Presently, the agent is focused on Maven projects below the directory in which it is run.
- Enhanced language support. See `SymbolSearch`
- Further effort to reduce token usage
- Integration with Spring repository to allow choice of project to work on
- Deep integration with GitHub to allow presentation of changes via PR
- Automated review of PRs
- (possible) chat mode to allow interaction with agent during a process flow

## Using this project

Run with the shell. The following commands are available. Note that some commands require a lengthy string to be
enclosed in double quotes.

- `set-focus <project>`: Focus on a project, e.g. `focus embabel-agent`. The string must match the last path segment of
  the project name, e.g. `embabel-agent-api`.
- `focus`: Show the current focus project.
- `x <description of code change operation>`: Make a code changeE.g.
  `x "Add a section in README.md explaining the FooBar Kotlin class"`.
- `chat`: Enter chat mode, where you can ask the agent to perform tasks such as explaining code or modifying it.

> Chat presently has no memory, so it will not remember the context of previous messages.