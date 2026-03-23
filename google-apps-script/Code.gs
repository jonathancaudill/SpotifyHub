/**
 * Google Apps Script — SpotifyHub Album Rating Receiver
 *
 * Deploy as a web app:
 *   Execute as: Me
 *   Who has access: Anyone
 *
 * Paste this into Extensions > Apps Script in your Google Sheet.
 */

function doPost(e) {
  try {
    var data = JSON.parse(e.postData.contents);

    var sheet = SpreadsheetApp.getActiveSpreadsheet().getSheets()[0];

    var today = Utilities.formatDate(new Date(), Session.getScriptTimeZone(), "yyyy-MM-dd");

    sheet.appendRow([
      data.albumCover  || "",   // A — Album Cover (image URL)
      data.artistName  || "",   // B — Artist Name
      data.title       || "",   // C — Title
      data.releaseDate || "",   // D — Release Date
      data.rating      || "",   // E — Rating
      "",                       // F — Review (left blank)
      "",                       // G — Review Title (left blank)
      today,                    // H — Review Date
    ]);

    return ContentService
      .createTextOutput(JSON.stringify({ success: true }))
      .setMimeType(ContentService.MimeType.JSON);

  } catch (err) {
    return ContentService
      .createTextOutput(JSON.stringify({ success: false, error: err.message }))
      .setMimeType(ContentService.MimeType.JSON);
  }
}

/**
 * GET handler — looks up an existing rating by artist + album title.
 *
 * Query params:
 *   ?action=lookup&artistName=...&title=...
 *
 * Returns:
 *   { found: true,  rating: 8.5 }   — if a matching row exists
 *   { found: false }                 — if no match
 */
function doGet(e) {
  try {
    var action = (e.parameter.action || "").toLowerCase();

    if (action === "lookup") {
      var artistName = (e.parameter.artistName || "").toLowerCase().trim();
      var title      = (e.parameter.title      || "").toLowerCase().trim();

      if (!artistName || !title) {
        return ContentService
          .createTextOutput(JSON.stringify({ found: false, error: "Missing artistName or title" }))
          .setMimeType(ContentService.MimeType.JSON);
      }

      var sheet = SpreadsheetApp.getActiveSpreadsheet().getSheets()[0];
      var data  = sheet.getDataRange().getValues();

      // Skip header row (row 0), search for matching artist (col B=1) + title (col C=2)
      for (var i = 1; i < data.length; i++) {
        var rowArtist = String(data[i][1]).toLowerCase().trim();
        var rowTitle  = String(data[i][2]).toLowerCase().trim();

        if (rowArtist === artistName && rowTitle === title) {
          var rating = data[i][4]; // col E = rating
          return ContentService
            .createTextOutput(JSON.stringify({ found: true, rating: rating }))
            .setMimeType(ContentService.MimeType.JSON);
        }
      }

      return ContentService
        .createTextOutput(JSON.stringify({ found: false }))
        .setMimeType(ContentService.MimeType.JSON);
    }

    // Default: health check
    return ContentService
      .createTextOutput(JSON.stringify({ status: "ok", message: "SpotifyHub rating endpoint is live." }))
      .setMimeType(ContentService.MimeType.JSON);

  } catch (err) {
    return ContentService
      .createTextOutput(JSON.stringify({ found: false, error: err.message }))
      .setMimeType(ContentService.MimeType.JSON);
  }
}
