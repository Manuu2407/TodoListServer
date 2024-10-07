fetch('http://localhost:8080/todos')
    .then(response => response.json())
    .then(data => {
        console.log(data);
        // Hier kannst du die Daten weiterverarbeiten und darstellen
    })
    .catch(error => console.error('Fehler:', error));

    const output = document.getElementById('output'); // Ein HTML-Element, wo die Daten angezeigt werden sollen

fetch('http://localhost:8080/todos')
    .then(response => response.json())
    .then(data => {
        output.innerHTML = JSON.stringify(data); // Hier wird die Daten als JSON angezeigt
    });
