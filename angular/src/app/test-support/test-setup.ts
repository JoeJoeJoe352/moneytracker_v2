// A tesztek futtatási környezete.
if (typeof Element !== 'undefined' && !Element.prototype.scrollIntoView) {
    // E nélkül minden scrollIntoView hívás egy setTimeout-on belül dobna egy kezeletlen kivételt
    Element.prototype.scrollIntoView = () => undefined;
}
