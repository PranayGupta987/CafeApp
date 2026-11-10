import fs from 'fs';
import path from 'path';

const envPath = path.resolve('.env.local');
const envContent = fs.readFileSync(envPath, 'utf8');
const match = envContent.match(/RENDER_API_KEY=(.*)/);
const apiKey = match[1].trim();

async function callRender(endpoint) {
  const url = `https://api.render.com/v1${endpoint}`;
  const res = await fetch(url, {
    headers: {
      'Accept': 'application/json',
      'Authorization': `Bearer ${apiKey}`
    }
  });
  return await res.json();
}

async function main() {
  const serviceId = 'srv-dbbqq5oae00c73aiefpg';

  console.log(`Polling latest deploy on service ${serviceId}...`);
  for (let i = 0; i < 40; i++) {
    const deploys = await callRender(`/services/${serviceId}/deploys?limit=2`);
    const latest = deploys[0]?.deploy;
    if (latest) {
      console.log(`[${new Date().toLocaleTimeString()}] Deploy ${latest.id} (${latest.trigger}): ${latest.status}`);
      if (latest.status === 'live') {
        console.log('\n🎉 SUCCESS! Service is LIVE on Render!');
        console.log('Public URL: https://cafeapp-backend-sgj7.onrender.com');
        return;
      }
      if (latest.status === 'build_failed' || latest.status === 'update_failed') {
        console.error('Deployment status failed:', latest.status, latest.id);
        // check if second deploy is running
        const second = deploys[1]?.deploy;
        if (second && (second.status === 'build_in_progress' || second.status === 'update_in_progress' || second.status === 'queued')) {
          console.log(`Watching second deploy ${second.id}...`);
        } else {
          process.exit(1);
        }
      }
    }
    await new Promise(r => setTimeout(r, 6000));
  }
}

main().catch(console.error);
