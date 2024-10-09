// loads every todo in given Todos<Array>
function loadTodos() {
  fetch("http://localhost:8080/todos")
    .then((response) => response.json())
    .then((data) => {
      const tableBody = document.querySelector("#todo-table tbody");
      tableBody.innerHTML = ""; // sets tableBody empty before loading Todos

      data.forEach((todo) => {
        const row = document.createElement("tr");

        const deleteRowCell = document.createElement("td");
        const deleteRowCheckbox = document.createElement("input");
        deleteRowCheckbox.type = "checkbox";
        deleteRowCheckbox.className = "deleteBox";
        deleteRowCheckbox.dataset.id = todo.id;
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
        checkbox.className = "updateBox";
        checkbox.checked = todo.completed;
        checkbox.dataset.id = todo.id;
        checkboxCell.appendChild(checkbox);
        row.appendChild(checkboxCell);

        tableBody.appendChild(row);
      });

      if (data.length > 0) {
        const lastTodoId = data[data.length - 1].id;
        addRowWithInputCells(lastTodoId + 1);
      } else {
        addRowWithInputCells(1); // if no Todos are given yet
      }
    })
    .catch((error) => {
      console.error("Fehler beim Abrufen der To-Dos:", error);
    });
}

// adding a row to end of table for input new todo
function addRowWithInputCells(lastTodoID) {
  const tableBody = document.querySelector("#todo-table tbody");
  const row = document.createElement("tr");

  // adds empty cell to row
  const emptyCell = document.createElement("td");
  emptyCell.textContent = "";
  row.appendChild(emptyCell);

  // adds idCell to row with numerically acsending ID
  const idCell = document.createElement("td");
  idCell.textContent = lastTodoID;
  row.appendChild(idCell);

  // adds text input Cell for new Task to row
  const taskCell = document.createElement("td");
  const taskInput = document.createElement("input");
  taskInput.type = "text";
  taskInput.placeholder = "Aufgabe eingeben";
  taskCell.appendChild(taskInput);
  row.appendChild(taskCell);

  // adds an empty Cell to row in Completed Collumn
  const completedCell = document.createElement("td");
  completedCell.textContent = "";
  row.appendChild(completedCell);

  // adds a Cell with button for adding task to todo list
  const actionCell = document.createElement("td");
  const saveButton = document.createElement("button");
  saveButton.id = "save-button";
  saveButton.textContent = "Todo hinzufügen";
  actionCell.appendChild(saveButton);
  row.appendChild(actionCell);

  // adds this new row to tablebody
  tableBody.appendChild(row);

  saveButton.addEventListener("click", addTodo);
}

// updates completed status for task, depending on checkbox status in collumn Set Completed
// and sends PUT request to http Server
function updateTodos() {
  const checkboxes = document.querySelectorAll(".updateBox:checked");

  // maps the given dataset of checkboxes by id and completed status
  const updates = Array.from(checkboxes).map((checkbox) => ({
    id: checkbox.dataset.id,
    completed: checkbox.checked,
  }));

  // sends PUT request to http Server with updated tasks
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

// adds a task to todolist and sends POST request to http Server
function addTodo(event) {
  const saveButton = event.target;
  const row = saveButton.closest("tr");
  const taskInput = row.querySelector("input[type='text']");

  // defines the new task by the value that has been inserted in taskInput field
  const newTodo = {
    task: taskInput.value,
  };

  if (newTodo.task === "") {
    return;
  } else {
    // sends POST request to http Server with new task
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
        return response.json();
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

// deletes tasks from list, depending on checkbox status in collumn Delete
// and sending DELETE request to http Server
function deleteTodos() {
  const deleteCheckboxes = document.querySelectorAll(".deleteBox:checked");

  // defines the Array with IDs from tasks to delete
  const deletes = Array.from(deleteCheckboxes).map((deleteCheckbox) => ({
    id: deleteCheckbox.dataset.id,
  }));

  // sends DELETE request to http Server one-by-one, as server handles DELETE requests by unique URL for the given task
  // Server can´t handle multiple tasks in one single DELETE request
  deletes.forEach((todo) => {
    fetch(`http://localhost:8080/todos/${todo.id}`, {
      method: "DELETE",
      mode: "cors",
      headers: {
        "Content-Type": "application/json",
      },
    })
      .then((response) => {
        if (!response.ok) {
          throw new Error("Fehler beim Löschen der To-Dos");
        }
        return response.text();
      })
      .then((data) => {
        console.log(`To-Do mit ID ${todo.id} erfolgreich gelöscht`);
        loadTodos();
      })
      .catch((error) => {
        console.error(
          `Fehler beim Löschen der To-Do mit ID ${todo.id}:`,
          error
        );
      });
  });
}

// makes sure to refresh To-Do List on every reload of the window
// buttons for delete and update todos are declared
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
