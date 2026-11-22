# Development Guidelines

## Environment
- Java: JDK 21.
- Build: committed Maven Wrapper in `java_code/`.
- Python: Python 3.11 in local `python_code/.venv/`.
- IDE: IntelliJ IDEA.
- Target developer OSes: macOS and Windows.

## Build commands
macOS/Linux:
```bash
./java_code/mvnw -f java_code/pom.xml clean test
```
Windows PowerShell:
```powershell
.\java_code\mvnw.cmd -f java_code\pom.xml clean test
```

## Git workflow
- Work on feature branches; merge through pull requests.
- Keep `main` buildable.
- Pull before starting and before opening a PR.
- Do not commit `.venv`, model weights, large datasets, generated image collections, secrets or local IDE state.
- Use relative paths and test on both operating systems.

## Code ownership
- Tarek: Java app, JavaFX, database/JDBC, orchestration and Python process integration.
- Ayoub: Python worker, YOLO detection, adversarial attacks and attack metrics.
- Shokor: Java OpenCV processing, CBIR and restoration algorithms.

## Quality
- Add tests or reproducible manual verification steps.
- Use fixed seeds and log parameters.
- Keep original images immutable and save derived images as new records.
- Document dependencies and run instructions.
