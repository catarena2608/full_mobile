module.exports = function normalizeDates(obj) {
  if (Array.isArray(obj)) {
    return obj.map(normalizeDates);
  } else if (obj && typeof obj === "object") {
    const newObj = {};
    for (let key in obj) {
      if (obj[key] && typeof obj[key] === "object" && "$date" in obj[key]) {
        newObj[key] = new Date(obj[key]["$date"]);
      } else {
        newObj[key] = normalizeDates(obj[key]);
      }
    }
    return newObj;
  }
  return obj;
}