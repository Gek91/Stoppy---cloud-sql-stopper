- Enable Cloud SQL Admin API
- Create pubsub topic
- App engine default service account with Cloud SQL ADMIN role
- message example 
{
"instanceId":"istance-test",
"action":"start"
}


gcloud functions deploy cloud-sql-stopper --entry-point functions.PubSubHandlerFunction --runtime java11 --trigger-topic cloud-sql-topic --region=europe-west1 --memory=128MB --max-instances=1 --set-env-vars GCP_PROJECT={CHAT WEBHOOK} 