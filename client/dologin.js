const fs=require("fs"), api=require("./api");
const sleep=ms=>new Promise(r=>setTimeout(r,ms));
(async()=>{
  const s=JSON.parse(fs.readFileSync(".session.json","utf8"));
  const auth=await api.authenticate(s.installationID||0, s.signStamp||0);
  console.log("bootstrap:",auth.status, JSON.stringify(auth.data));
  if(auth.status!==200 || !auth.data || !auth.data.accessToken){
    console.log("-> bootstrap not ready (likely still rate-limited); stopping before OTP.");
    return;
  }
  s.token=auth.data.accessToken; s.refreshToken=auth.data.refreshToken;
  if(auth.data.installationID) s.installationID=auth.data.installationID;
  if(auth.data.signStamp) s.signStamp=auth.data.signStamp;
  fs.writeFileSync(".session.json",JSON.stringify(s,null,2));
  await sleep(6000);
  const otp=await api.sendLoginOTP(s.mobile, s.installationID, s.token);
  console.log("send OTP:",otp.status, JSON.stringify(otp.data));
  console.log(otp.status===200 ? "\n*** OTP SENT — check your phone ***" : "\n(send failed)");
})().catch(e=>console.error("ERR",e.message));
