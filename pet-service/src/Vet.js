require('dotenv').config();
const express = require('express');
const mongoose = require('mongoose');
const app = express();
const port = 3000;

app.use(express.json());

mongoose.connect(process.env.MONGO_DB_URI, {
    useNewUrlParser: true,
})
    .then(() => console.log('MongoDB connected'))
    .catch(err => console.error('MongoDB connection error:', err));

const vetSchema = mongoose.model('Vet', new mongoose.Schema({
    firstName: String,
    lastName: String,
    specialties: [String]
}));

app.get('/vets', async (req, res) => {
    try {
        const vets = await vetSchema.find();
        console.log("Found vets:", vets);
        res.json(vets);
    } catch (err) {
        res.status(500).send('Error retrieving vets');
    }
});

app.get('/vets/:id', async (req, res) => {
    try {
        const vet = await vetSchema.findById(req.params.id);
        if (!vet) {
            return res.status(404).send('Vet not found');
        }
        res.json(vet);
    } catch (err) {
        res.status(500).send('Error retrieving vet');
    }
});

app.post('/vets', async (req, res) => {
    try {
        const vet = new vetSchema(req.body);
        await vet.save();
        res.status(201).json(vet);
    } catch (err) {
        res.status(400).send('Error creating vet');
    }
});

app.put('/vets/:id', async (req, res) => {
    let vet = await vetSchema.findByIdAndUpdate(req.params.id, req.body, { new: true });
    res.json(vet);
});

app.delete('/vets/:id', async (req, res) => {
    await vetSchema.findByIdAndDelete(req.params.id);
    res.status(204).send();
});

app.listen(port, () => {
    console.log(`Vet service listening at http://localhost:${port}`);
});
