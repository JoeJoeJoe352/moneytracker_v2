// jsdom (a tesztek futtatási környezete) nem implementálja a scrollIntoView-t,
// pedig több komponens is meghívja UX célból (pl. új sor hozzáadásakor görgetés).
// E nélkül a hívás egy setTimeout-on belül dob egy kezeletlen kivételt, ami
// "false positive"-ként megbuktatja a `ng test` futást a tesztek sikeressége mellett is.
if (typeof Element !== 'undefined' && !Element.prototype.scrollIntoView) {
    Element.prototype.scrollIntoView = () => undefined;
}
