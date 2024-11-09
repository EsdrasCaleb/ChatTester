from huggingface_hub import hf_hub_download, snapshot_download, login
from transformers import AutoModelForCausalLM, AutoTokenizer    
import os
import json
import shutil



def download_model_for_djl(
    repo_id,
    model_name,
    output_dir,
    auth_token=None,  # New parameter for auth token
    revision="main",
    include_files=None
):
    return False
    """
    Downloads a model from Hugging Face Hub and organizes it for DJL consumption.
    
    Args:
        repo_id (str): Hugging Face model repository ID (e.g., 'bert-base-uncased')
        model_name (str): Name to use for the model in DJL
        output_dir (str): Base directory to store the downloaded model
        auth_token (str): Hugging Face API token for authentication
        revision (str): Model revision/tag to download
        include_files (List[str]): Specific files to download, if None downloads all
    """
    # Authenticate if token is provided
    if auth_token:
        login(token=auth_token)
    
    # Create model directory structure
    model_dir = os.path.join(output_dir, model_name)
    os.makedirs(model_dir, exist_ok=True)
    
    # Download model files
    if include_files:
        # Download specific files
        for file in include_files:
            downloaded_path = hf_hub_download(
                repo_id=repo_id,
                filename=file,
                revision=revision,
                cache_dir=model_dir,
                token=auth_token  # Pass token for authentication
            )
            # Move file to model directory
            target_path = os.path.join(model_dir, os.path.basename(file))
            if downloaded_path != target_path:
                shutil.move(downloaded_path, target_path)
    else:
        # Download entire repository
        snapshot_download(
            repo_id=repo_id,
            revision=revision,
            cache_dir=model_dir,
            local_dir=model_dir,
            token=auth_token  # Pass token for authentication
        )
    
    # Create model metadata for DJL
    metadata = {
        "metadataVersion": "0.2",
        "resourceType": "model",
        "application": "nlp",
        "groupId": "ai.djl.huggingface",
        "artifactId": model_name,
        "name": model_name,
        "version": "0.0.1",
        "repository": f"https://huggingface.co/{repo_id}"
    }
    
    # Save metadata
    with open(os.path.join(output_dir, "serving.properties"), "w") as f:
        for key, value in metadata.items():
            f.write(f"{key}={value}\n")

# Example usage
if __name__ == "__main__":
    # Your Hugging Face token
    HF_TOKEN = "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"  # Replace with your actual token
    
    model_name = "meta-llama/Llama-3.2-3B-Instruct"  # Example model on Hugging Face
    model = AutoModelForCausalLM.from_pretrained(model_name, token=HF_TOKEN)
    tokenizer = AutoTokenizer.from_pretrained(model_name, token=HF_TOKEN)

    model.save_pretrained("models/llama-3.2-3b-Instruct")
    tokenizer.save_pretrained("models/llama-2-7b-Instruct")




    # Example for downloading BERT model with authentication
    download_model_for_djl(
        repo_id="meta-llama/Llama-3.2-3B-Instruct",
        model_name="meta-llama32b-instruct",
        output_dir="models",
        auth_token=HF_TOKEN,  # Pass your token here
    )