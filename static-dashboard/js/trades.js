(function () {
  "use strict";

  const table = document.getElementById("trades-table");
  const tbody = document.getElementById("trades-tbody");
  if (!table || !tbody) return; // not on this page
  let rows = [];

  // ---------- sortable columns ----------
  table.querySelectorAll("thead th").forEach((th) => {
    th.addEventListener("click", (e) => {
      if (e.target.classList.contains("resize-handle")) return; // ignore resize clicks
      const col = th.dataset.col;
      const type = th.dataset.type || "string";
      const dir =
        th.getAttribute("aria-sort") === "ascending"
          ? "descending"
          : "ascending";

      // clear all, set this one
      table
        .querySelectorAll("thead th")
        .forEach((o) => o.removeAttribute("aria-sort"));
      th.setAttribute("aria-sort", dir);

      const mult = dir === "ascending" ? 1 : -1;
      rows.sort((a, b) => {
        const av = a[col],
          bv = b[col];
        if (type === "number") return (Number(av) - Number(bv)) * mult;
        return String(av).localeCompare(String(bv)) * mult;
      });
      renderRows();
    });
  });

  // ---------- resizable columns ----------
  table.querySelectorAll(".resize-handle").forEach((handle) => {
    handle.addEventListener("mousedown", (e) => {
      e.preventDefault();
      const th = handle.closest("th");
      const startX = e.clientX;
      const startWidth = th.offsetWidth;

      // Listen on DOCUMENT so the drag survives leaving the handle.
      function onMove(ev) {
        th.style.width = startWidth + ev.clientX - startX + "px";
      }
      function onUp() {
        document.removeEventListener("mousemove", onMove);
        document.removeEventListener("mouseup", onUp);
      }
      document.addEventListener("mousemove", onMove);
      document.addEventListener("mouseup", onUp);
    });
  });

  function renderRows() {
    if (!rows.length) {
      tbody.innerHTML = `<tr><td colspan="5" class="text-center">No trades found</td></tr>`;
      return;
    }
    tbody.innerHTML = rows
      .map(
        (r) => `
      <tr>
        <td>${r.tradeRef}</td><td>${r.symbol}</td>
        <td>${r.quantity}</td><td>${r.price}</td>
        <td>${r.status}</td>
      </tr>`,
      )
      .join("");
  }

  // initial load — hits the REST API from Day 5
  fetch("/api/v1/trades?size=200")
    .then((r) => r.json())
    .then((data) => {
      rows = data.content || data;
      renderRows();
    })
    .catch((err) => {
      console.error("Error fetching trades:", err);
      tbody.innerHTML = `<tr><td colspan="5" class="text-center text-danger">Error loading trades</td></tr>`;
    });
})();
