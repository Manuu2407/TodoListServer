function loadTodos() {
  fetch("http://localhost:8080/todos")
    .then((response) => response.json())
    .then((data) => {
      const tableBody = document.querySelector("#todo-table tbody");
      tableBody.innerHTML = ""; // Vorherige Todos löschen, um nur die neuen Todos zu laden

      data.forEach((todo) => {
        const row = document.createElement("tr");

        const deleteRowCell = document.createElement("td");
        const deleteRowCheckbox = document.createElement("input");
        deleteRowCheckbox.type = "checkbox";
        deleteRowCell.appendChild(deleteRowCheckbox);
        row.appendChild(deleteRowCell);

        const idCell = document.createElement("td");
        idCell.textContent = todo.id;
        row.appendChild(idCell);

        const taskCell = document.createElement("td");
        taskCell.textContent = todo.task;
        row.appendChild(taskCell);

        const completedCell = document.createElement("td");
        completedCell.textContent = todo.completed ? "Yes" : "No";
        row.appendChild(completedCell);

        const checkboxCell = document.createElement("td");
        const checkbox = document.createElement("input");
        checkbox.type = "checkbox";
        checkbox.checked = todo.completed;
        checkbox.dataset.id = todo.id; // Store the ID in a data attribute
        checkboxCell.appendChild(checkbox);
        row.appendChild(checkboxCell);

        tableBody.appendChild(row);
      });

      // Füge die neue Zeile für Input-Felder hinzu, nachdem die vorhandenen Todos geladen wurden
      addRowWithInputCells(data.length);
    })
    .catch((error) => {
      console.error("Fehler beim Abrufen der To-Dos:", error);
    });
}

function addRowWithInputCells(length) {
  const tableBody = document.querySelector("#todo-table tbody");
  const row = document.createElement("tr");

  const emptyCell = document.createElement("td");
  emptyCell.textContent = "";
  row.appendChild(emptyCell);
  // Füge ein leeres Feld für die ID hinzu
  const idCell = document.createElement("td");
  idCell.textContent = length + 1;
  row.appendChild(idCell);

  // Füge ein Eingabefeld für die Aufgabe hinzu
  const taskCell = document.createElement("td");
  const taskInput = document.createElement("input");
  taskInput.type = "text";
  taskInput.placeholder = "Aufgabe eingeben";
  taskCell.appendChild(taskInput);
  row.appendChild(taskCell);

  // Füge ein Eingabefeld für den Status "erledigt" hinzu
  const completedCell = document.createElement("td");
  const completedInput = document.createElement("input");
  completedInput.type = "checkbox";
  completedCell.appendChild(completedInput);
  row.appendChild(completedCell);

  // Füge ein weiteres Feld für Aktionen (z. B. Speichern) hinzu
  const actionCell = document.createElement("td");
  const saveButton = document.createElement("button");
  saveButton.id = "save-button";
  saveButton.textContent = "Speichern";
  actionCell.appendChild(saveButton);
  row.appendChild(actionCell);

  // Füge die neue Zeile in den Tabellen-Body ein
  tableBody.appendChild(row);

  saveButton.addEventListener("click", addTodo);
}

function updateTodos() {
  const checkboxes = document.querySelectorAll(
    "#todo-table tbody input[type='checkbox']"
  );
  const updates = Array.from(checkboxes).map((checkbox) => ({
    id: checkbox.dataset.id,
    completed: checkbox.checked,
  }));

  fetch("http://localhost:8080/todos", {
    method: "PUT",
    mode: "cors",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(updates),
  })
    .then((response) => {
      if (!response.ok) {
        throw new Error("Fehler beim Aktualisieren der To-Dos");
      }
      return response.json();
    })
    .then((data) => {
      console.log("To-Dos erfolgreich aktualisiert:", data);
      loadTodos();
    })
    .catch((error) => {
      console.error("Fehler beim Aktualisieren der To-Dos:", error);
    });
}

function addTodo(event) {
  const saveButton = event.target;
  const row = saveButton.closest("tr");
  const taskInput = row.querySelector("input[type='text']");
  const completedInput = row.querySelector("input[type='checkbox']");

  const newTodo = {
    task: taskInput.value,
    completed: completedInput.checked,
  };

  if (newTodo.task === "") {
    return;
  } else {
    fetch("http://localhost:8080/todos", {
      method: "POST",
      mode: "cors",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(newTodo),
    })
      .then((response) => {
        if (!response.ok) {
          throw new Error("Fehler beim Hinzufügen des Todos!");
        }
        return response.text();
      })
      .then((data) => {
        console.log("To-Do erfolgreich hinzugefügt:", data);
        loadTodos();
      })
      .catch((error) => {
        console.error("Fehler beim Hinzufügen des To-Dos:", error);
      });
  }
}

function deleteTodos() {
  // Hier weitermachen
}

window.onload = () => {
  loadTodos();
  addRowWithInputCells();

  const updateButton = document.querySelector("#update-button");
  if (updateButton) {
    updateButton.addEventListener("click", updateTodos);
  } else {
    console.error("Update-Button nicht gefunden");
  }

  const deleteButton = document.querySelector("#delete-button");
  if (deleteButton) {
    deleteButton.addEventListener("click", deleteTodos);
  } else {
    console.error("Delete-Button nicht gefunden");
  }
};
