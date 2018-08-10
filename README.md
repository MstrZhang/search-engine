# UTSC Search Engine

## Overview

This is a simple search engine application developed for the CSCC01 course at UTSC
It was primarily developed by myself with assistance from my team members (Jeff Xu, Edgar Lai, and Angela Zhu)

The idea of this project is for students and instructors to upload and search for various files in a file system. A user can
make an account on the platform for some additional features but unregistered users still have access to the basic search
functionality. The project's simple code base also allows for easy scaling for adding new features

The project features the use of the following technologies:

  - Backend Technologies:
    - Tomcat
    - Apache Lucene
    - SQLite
    - Mockito and JUnit

  - Frontend Technologies:
    - Angular 5
    - Bootstrap 4

The team and I spent 7 weeks developing this project with the following being the end result

---

## Installation

Prerequisites:

  - NodeJS
  - Angular CLI
  - Maven
  - Java

Building:

  - Frontend:
    - `cd` to `src/main/webapp/`
    - `npm install`
    - Build with `ng serve -o`
      - App opens on `http://localhost:4200`

  - Backend:
    - Build with `mvn clean install tomcat7:run`
    - Backend runs on `http://localhost:8080`

