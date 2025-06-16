new Vue({
    el: "#eventos",
    data: {
        username: "",
        password: "",
        showEvent: false,
        showForm: false,
        showNoEvent: false,
        showCover: true,
        showDelete: false,
        showEventSaved: false,
        showEventDeleted: false,
        searchDate: "",
        establishment: "",
        inputEstablishment: "",
        address: "",
        inputAddress: "",
        date: "",
        inputDate: "",
        content: "",
        inputContent: "",
        poster: "",
        inputPoster: null,
        map: "",
        inputMap: "",
        file: null,
        selectedEvents: [],
        events: [
            {
                establishment: "El mesoncito",
                address: "C. Aduana, 3, 13500 Puertollano, Ciudad Real",
                date: "2025-01-01",
                content: "Fiesta Mexicana",
                poster: '<img src="../img/imagen1.jpg" alt="evento imagen" style="width: 400px; height: 400px;">',
                map: '<iframe src="https://www.google.com/maps/embed?pb=!1m18!1m12!1m3!1d3114.351416594194!2d-4.11284152355524!3d38.68677135910375!2m3!1f0!2f0!3f0!3m2!1i1024!2i768!4f13.1!3m3!1m2!1s0xd6b8cf53e9f8d2b%3A0xeedf97f108f04ee2!2sBar%20El%20Mesoncito!5e0!3m2!1ses!2ses!4v1733933838752!5m2!1ses!2ses" width="400" height="300" style="border:0;" allowfullscreen="" loading="lazy" referrerpolicy="no-referrer-when-downgrade"></iframe>'
            },
            {
                establishment: "Restaurante HAVANA",
                address: "Paseo de San Gregorio, S/N, 13500 Puertollano, Ciudad Real",
                date: "2025-02-01",
                content: "Monologo Danni Robira",
                poster: '<img src="../img/imagen2.jpg" alt="evento imagen" style="width: 400px; height: 400px;">',
                map: '<iframe src="https://www.google.com/maps/embed?pb=!1m18!1m12!1m3!1d3114.274502406314!2d-4.109504855540716!3d38.6885383902203!2m3!1f0!2f0!3f0!3m2!1i1024!2i768!4f13.1!3m3!1m2!1s0xd6b8d951c6cd995%3A0x6023f1902f9b8560!2sRestaurante%20HAVANA!5e0!3m2!1ses!2ses!4v1733933986816!5m2!1ses!2ses" width="400" height="300" style="border:0;" allowfullscreen="" loading="lazy" referrerpolicy="no-referrer-when-downgrade"></iframe>'
            },
            {
                establishment: "Restaurante Asiático NAKAMA",
                address: "C. Vía Crucis, 19, 13500 Puertollano, Ciudad Real",
                date: "2025-03-01",
                content: "Exhibición de tapas vegetarianas",
                poster: '<img src="../img/imagen3.jpg" alt="evento imagen" style="width: 400px; height: 400px;">',
                map: '<iframe src="https://www.google.com/maps/embed?pb=!1m18!1m12!1m3!1d3114.4604849300454!2d-4.112005365112297!3d38.684265499999995!2m3!1f0!2f0!3f0!3m2!1i1024!2i768!4f13.1!3m3!1m2!1s0xd6b8d274653c52b%3A0x586b94b2d3ae47f3!2sRestaurante%20Asi%C3%A1tico%20NAKAMA!5e0!3m2!1ses!2ses!4v1733934626820!5m2!1ses!2ses" width="400" height="300" style="border:0;" allowfullscreen="" loading="lazy" referrerpolicy="no-referrer-when-downgrade"></iframe>'
            },
            {
                establishment: "Auditorio Municipal",
                address: "Pl. Mariana Pineda, 0, 13500 Puertollano, Ciudad Real",
                date: "2025-04-01",
                content: "Rock la Mancha Festival",
                poster: '<img src="../img/imagen4.jpg" alt="evento imagen" style="width: 400px; height: 400px;">',
                map: '<iframe src="https://www.google.com/maps/embed?pb=!1m18!1m12!1m3!1d3114.1514033241547!2d-4.109704738305717!3d38.69136633423611!2m3!1f0!2f0!3f0!3m2!1i1024!2i768!4f13.1!3m3!1m2!1s0xd6b8cf6636e3549%3A0x4b13ec5f916e923c!2sAuditorio%20Municipal!5e0!3m2!1ses!2ses!4v1733934475151!5m2!1ses!2ses" width="400" height="300" style="border:0;" allowfullscreen="" loading="lazy" referrerpolicy="no-referrer-when-downgrade"></iframe>'
            },
            {
                establishment: "Museo Cristina García Rodero",
                address: "Pl. Constitución, s/n, 13500 Puertollano, Ciudad Real",
                date: "2025-05-01",
                content: "Exposición de Arte",
                poster: '<img src="../img/imagen5.jpg" alt="evento imagen" style="width: 400px; height: 400px;">',
                map: '<iframe src="https://www.google.com/maps/embed?pb=!1m18!1m12!1m3!1d389.2939734827675!2d-4.1110539430529505!3d38.68676282940689!2m3!1f0!2f0!3f0!3m2!1i1024!2i768!4f13.1!3m3!1m2!1s0xd6b8c5ff5e8e3b3%3A0x5dbac4a9911589ec!2sMuseo%20Cristina%20Garc%C3%ADa%20Rodero!5e0!3m2!1ses!2ses!4v1733934734205!5m2!1ses!2ses" width="400" height="300" style="border:0;" allowfullscreen="" loading="lazy" referrerpolicy="no-referrer-when-downgrade"></iframe>'
            }
        ]
    },
    methods: {
        validateAdmin() {
            if (this.username === "alexander" && this.password === "Abc1234") {
                window.location.href = 'views/administrador.html';
            } else {
                alert("Usuario o contraseña incorrecta, inténtalo de nuevo.");
            }
        },
        searchEvent() {
            let eventFound = false;
            for (let i = 0; i < this.events.length; i++) {
                if (this.searchDate === this.events[i].date) {
                    this.establishment = this.events[i].establishment;
                    this.address = this.events[i].address;
                    this.date = this.events[i].date;
                    this.content = this.events[i].content;
                    this.poster = this.events[i].poster;
                    this.map = this.events[i].map;
                    eventFound = true;
                }
            }

            if (eventFound) {
                this.showEvent = true;
                this.showCover = false;
                this.showNoEvent = false;
            } else {
                this.showEvent = false;
                this.showCover = false;
                this.showNoEvent = true;
            }

            this.showForm = false;
            this.showDelete = false;
            this.showEventSaved = false;
            this.showEventDeleted = false;
        },
        addEvent() {
            this.showForm = !this.showForm;
            this.showEvent = false;
            this.showCover = false;
            this.showNoEvent = false;
            this.showDelete = false;
            this.showEventSaved = false;
            this.showEventDeleted = false;
        },
        submitForm() {
            if (this.inputMap && !this.inputMap.includes('google.com/maps/embed')) {
                alert('Por favor, ingresa un código de mapa válido de Google Maps');
                return;
            }

            this.events.push({
                establishment: this.inputEstablishment,
                address: this.inputAddress,
                date: this.inputDate,
                content: this.inputContent,
                poster: this.inputPoster || '',
                map: this.processMap(this.inputMap)
            });

            this.inputEstablishment = "";
            this.inputAddress = "";
            this.inputDate = "";
            this.inputContent = "";
            this.inputPoster = "";
            this.inputMap = "";

            this.showForm = false;
            this.showEventSaved = true;
        },
        deleteEvent() {
            this.showDelete = true;
            this.showCover = false;
            this.showEvent = false;
            this.showNoEvent = false;
            this.showForm = false;
            this.showEventSaved = false;
            this.showEventDeleted = false;
        },
        deleteSelectedEvents() {
            const indexes = Object.keys(this.selectedEvents).filter(i => this.selectedEvents[i]);
            indexes.sort((a, b) => b - a);
            indexes.forEach(i => {
                this.events.splice(parseInt(i), 1);
                this.selectedEvents.splice(parseInt(i), 1);
            });

            if (indexes.length > 0) {
                this.showEventDeleted = true;
                this.showDelete = false;
            }
        },
        formatDate(date) {
            if (!date) return '';
            const [year, month, day] = date.split('-');
            return `${day}/${month}/${year.slice(2)}`;
        },
        processMap(iframe) {
            if (!iframe) return '';
            if (iframe.includes('google.com/maps/embed')) {
                return iframe.replace(/width="[^"]*"/i, 'width="400"')
                    .replace(/height="[^"]*"/i, 'height="300"');
            }
            return iframe;
        },
        handleFileUpload(event) {
            const file = event.target.files[0];
            if (file) {
                const validImageTypes = ['image/jpeg', 'image/png', 'image/gif'];
                if (validImageTypes.includes(file.type)) {
                    const reader = new FileReader();
                    reader.onload = (e) => {
                        const img = new Image();
                        img.onload = () => {
                            let width = img.width;
                            let height = img.height;
                            const maxWidth = 400;
                            const maxHeight = 400;

                            if (width > height) {
                                if (width > maxWidth) {
                                    height = Math.round((height * maxWidth) / width);
                                    width = maxWidth;
                                }
                            } else {
                                if (height > maxHeight) {
                                    width = Math.round((width * maxHeight) / height);
                                    height = maxHeight;
                                }
                            }

                            this.inputPoster = `<img src="${e.target.result}" alt="evento imagen" style="width: ${width}px; height: ${height}px;">`;
                        };
                        img.src = e.target.result;
                    };
                    reader.readAsDataURL(file);
                } else {
                    alert("Por favor, selecciona un archivo de imagen válido (JPEG, PNG, GIF).");
                }
            }
        }
    }
});