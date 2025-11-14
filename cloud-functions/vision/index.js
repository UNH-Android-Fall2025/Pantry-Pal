const vision = require('@google-cloud/vision');

const client = new vision.ImageAnnotatorClient();

exports.detectPantryItems = async (req, res) => {
  res.set('Access-Control-Allow-Origin', '*');
  if (req.method === 'OPTIONS') {
    res.set('Access-Control-Allow-Methods', 'POST');
    res.set('Access-Control-Allow-Headers', 'Content-Type');
    return res.status(204).send('');
  }

  try {
    const { imageBase64 } = req.body || {};
    if (!imageBase64) {
      return res.status(400).json({ error: 'imageBase64 missing' });
    }

    const [result] = await client.objectLocalization({
      image: { content: imageBase64 },
    });

    const objects = (result.localizedObjectAnnotations || []).map((obj) => ({
      name: obj.name,
      score: obj.score,
      vertices: obj.boundingPoly?.normalizedVertices || [],
    }));

    res.json({ objects });
  } catch (error) {
    console.error('Vision API error', error);
    res.status(500).json({ error: error.message });
  }
};

